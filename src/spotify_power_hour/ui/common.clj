(ns spotify-power-hour.ui.common
  (:require [spotify-power-hour.config.general :as cfg-gen]))

(def app-state (atom {}))

(def ui-scene-login :login-form-scene)
(def ui-scene-power-hour-main :power-hour-main)
(def ui-scene-power-hour-loading :power-hour-loading)
(def ui-scene-power-hour-start :power-hour-start)

(def ui-ph-song-count-defaults (vec (reverse (drop 1 (take-nth 5 (range 0 (inc cfg-gen/max-power-hour-length-min)))))))

(def ui-states
  {:login {:sending-request-to-auth-server :sending-request-to-auth-server
           :successfully-authed :successfully-authed
           :failed-to-auth :failed-to-auth}
   :logout {:logout :logout}
   :ph {:retrieving-song-starts :retrieving-song-starts
        :ready-to-start :ready-to-start}
   })

(defn get-panel [& panel-kws]
  (if-let [panel (get-in @app-state (apply conj [:panels] panel-kws))]
    panel
    (throw (ex-info (format "Could not find panel")
                    {:panel-kws      panel-kws
                     :current-panels (-> (get-in @app-state [:panels]) keys)}))))
