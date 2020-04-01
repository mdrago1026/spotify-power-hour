(ns spotify-power-hour.ui.controller
  (:require [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.common :as ui-cmn]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.common :as cmn-ui]))

(defn menu-account-logout-handler [e]
  (info "Logging out!")
  (reset! cmn-session/spotify-session {})
  (swap! ui-cmn/app-state assoc
         :authenticated? false
         :token nil
         :status (get-in cmn-ui/ui-states [:logout :logout])))
