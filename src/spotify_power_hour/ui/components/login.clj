(ns spotify-power-hour.ui.components.login
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [clojure.java.io :as io]
            [spotify-power-hour.ui.config :as cfg-ui]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.spotify.api :as api-spotify]
            [clojure.java.browse :as browse]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.controller.spotify :as ctrl-spotify]
            [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.components.common :as cmn-comp])
  (:import (java.awt.event KeyEvent)
           (javax.swing ImageIcon)))

(defn verify-login [redir-url]
  (try
    {:valid? true
     :response (ctrl-spotify/attempt-to-validate-oauth redir-url cmn-session/local-session-id)}
    (catch Exception e
      {:valid? false
       :response (ex-data e)})))

(defn handle-client-id-submit [e]
  (future
    (let [client-id (text (select (to-root e) [:#login-client-id]))
          redir-uri (text (select (to-root e) [:#login-redirect-uri]))
          oauth-url (api-spotify/client-id->oauth-url client-id redir-uri cmn-session/local-session-id)]
      (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]))
      (info "OAUTH URL: " oauth-url)
      (browse/browse-url oauth-url)
      (let [{:keys [valid? response]} (verify-login redir-uri)]
        (if valid?
          (do
            (info "Successfully authenticated!")
            (reset! cmn-session/spotify-session (:token response))
            (let [{:keys [id] :as user-info} (api-spotify/my-profile)
                  new-text (format "Logged in as: %s" id)]
              (cmn-comp/update-shared-user-info-component new-text))
            (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :successfully-authed])
                   :authenticated? true
                   :scene cmn-ui/ui-scene-power-hour-main))
          (do
            (info "Failed to retrieve user details with error: " response)
            (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :failed-to-auth])
                   :authenticated? false)))))))

(defn
  handle-keys
  [e]
  (condp = (.getKeyCode e)
    KeyEvent/VK_ENTER (do
                        (handle-client-id-submit e)
                        nil)
    ;(when-not (= (get-in config/ui-states [:auth :authenticating])
    ;             (:status @cmn/app-state))
    ;  (info "Pressed Enter")
    ;  ;;(login-handler e))
    ;  nil)
    nil
    ))

(def login-form
  (mig/mig-panel
    :constraints ["fill"]
    :items [[(label "Client ID") "cell 0 0, grow"]
            [(text :editable? true
                   :id :login-client-id
                   :class :login-form
                   :text (or (System/getenv "SPH_DEFAULT_CLIENT_ID") "")
                   :columns 20) "cell 0 0"]
            [(label "Redirect URI") "cell 0 1, grow"]
            [(text :editable? true
                   :id :login-redirect-uri
                   :class :login-form
                   :text (or (System/getenv "SPH_DEFAULT_REDIRECT_URI") "")
                   :columns 20) "cell 0 1"]
            [(button :text "Login"
                     :class :login-form
                     :listen [:action handle-client-id-submit]
                     :id :login-button)
             "cell 0 2, align center"]
            [(label :icon (ImageIcon. (io/resource "ajax-loader.gif"))
                    :id :login-spinner :visible? false)
             "cell 0 3, align center"]

            ;; left off: make the error message show up!
            [(label :text "Failed to login. Please try again."
                    :foreground (color "#ff0000")
                    :visible? false
                    :id :login-failed-text) "cell 0 4, align center"]
            [(label :text "Successfully logged in!"
                    :foreground (color "#03fc0f")
                    :visible? false
                    :id :login-success-text) "cell 0 5, align center"]
            ]))

(defn login-panel []
  (mig/mig-panel
    :id :login-panel
    :constraints ["fill, flowy"]
    :items [
            ;;[slack-top-info-panel "cell 0 0, aligny top, growx"]
            [login-form "cell 0 0, align center"]
            ;;[(btm-info-pane) "cell 0 2, grow"]
            ]))
