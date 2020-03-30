(ns spotify-client.ui.components.login
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [clojure.java.io :as io]
            [spotify-client.ui.config :as cfg-ui]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-client.spotify.api :as api-spotify]
            [clojure.java.browse :as browse]
            [spotify-client.ui.common :as cmn-ui])
  (:import (java.awt.event KeyEvent)
           (javax.swing ImageIcon)))

(defn handle-client-id-submit [e]
  (info "HELLO")
    (let [client-id (text (select (to-root e) [:#login-client-id]))
          _ (info "CLIENT IT: "client-id)
          oauth-url (api-spotify/client-id->oauth-url client-id)]
      (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]))
      (info "OAUTH URL: "oauth-url)
      (browse/browse-url oauth-url)))

(defn
  handle-keys
  [e]
  (condp = (.getKeyCode e)
    KeyEvent/VK_ENTER  (do
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
                   :text "ff1826b82af24af9b95d0a951a676ab5"
                   :columns 20) "cell 0 0"]
            [(label :icon (ImageIcon. (io/resource "ajax-loader.gif"))
                    :id :login-spinner :visible? false)
             "cell 0 5, align center"]
            [(label :text "Incorrect credentials. Please try again."
                    :foreground (color "#ff0000")
                    :visible? false
                    :id :login-incorrect-text) "cell 0 6, align center"]
            [(button :text "Login"
                     :class :login-form
                     :listen [:action handle-client-id-submit]
                     :id :login-button)
             "cell 0 4, align center"]
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
