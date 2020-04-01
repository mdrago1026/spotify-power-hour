(ns spotify-power-hour.ui.common)

(def app-state (atom {}))

(def ui-scene-login :login-form-scene)
(def ui-scene-power-hour-main :power-hour-main)

(def ui-states
  {:login {:sending-request-to-auth-server :sending-request-to-auth-server
           :successfully-authed :successfully-authed
           :failed-to-auth :failed-to-auth}
   :logout {:logout :logout}
   })

(defn get-panel [& panel-kws]
  (if-let [panel (get-in @app-state (apply conj [:panels] panel-kws))]
    panel
    (throw (ex-info (format "Could not find panel")
                    {:panel-kws      panel-kws
                     :current-panels (-> (get-in @app-state [:panels]) keys)}))))
