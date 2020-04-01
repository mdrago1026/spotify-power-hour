(ns spotify-power-hour.ui.controller
  (:require [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.common :as ui-cmn]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.ui.components.common :as cmn-comp]
            [seesaw.mig :as mig]
            [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [spotify-power-hour.spotify.api :as api-spotify]
            [clojure.java.browse :as browse]
            [spotify-power-hour.controller.spotify :as ctrl-spotify]))

;;; LOGOUT

(defn menu-account-logout-handler [e]
  (info "Logging out!")
  (reset! cmn-session/spotify-session {})
  (cmn-comp/update-shared-user-info-component cmn-comp/user-info-not-logged-in)
  (swap! ui-cmn/app-state assoc
         :authenticated? false
         :token nil
         :scene cmn-ui/ui-scene-login
         :status (get-in cmn-ui/ui-states [:logout :logout])))


;; LOGIN

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


;; CALLBACKS

(defn init-ph-main-scene []
  (info "Power Hour Main Scene INIT Callback FUNC!"))
