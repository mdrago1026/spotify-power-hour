(ns spotify-power-hour.ui.controller
  (:require [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.common :as ui-cmn]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.ui.components.common :as cmn-comp]))

(defn menu-account-logout-handler [e]
  (info "Logging out!")
  (reset! cmn-session/spotify-session {})
  (cmn-comp/update-shared-user-info-component cmn-comp/user-info-not-logged-in)
  (swap! ui-cmn/app-state assoc
         :authenticated? false
         :token nil
         :scene cmn-ui/ui-scene-login
         :status (get-in cmn-ui/ui-states [:logout :logout])))
