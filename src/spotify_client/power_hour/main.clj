(ns spotify-client.power-hour.main
  (:require [spotify-client.spotify.api :as api-spotify]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.common.session :as cmn-session]))

(def power-hour-seconds 60)

;; for testing
;(def halo-night-songs
;  (api-spotify/handle-paginated-requests
;    (format (:playlists-list-songs api-spotify/urls)
;            (-> (api-spotify/filter-playlist-by-name
;                  "HALO nighT"
;                  (api-spotify/handle-paginated-requests
;                    (format (:playlists-get api-spotify/urls) "mdrago1026")))
;                first :id))))




(defn is-section-valid-for-ph? [total-song-ms {:keys [start duration] :as section-obj}]
  (let [start-ms (* 1000 start)
        duration-ms (* 1000 duration)
        ;;end-ms (+ start-ms duration-ms)
        power-hour-duration (* 1000 power-hour-seconds)]
    (if (<= power-hour-duration duration-ms)                ;; if this loud section is 60 sec or greater, we know we are good
      true
      (if (<= power-hour-duration (- total-song-ms start-ms)) ;; if we have at least 60 seconds from the start of loud to end of song
        true
        false))))

;; 4 min = 240000

;;(is-section-valid-for-ph? 240000 {:start 181.00 :duration 30.00})
;; say we have a 4:00 min song
;; loud section starts at 3:10
;; (total-song-ms - loud-section-start) >= 60, then we good
;; this won't work as we need a 60 sec section


(defn track-info->get-start-time [{duration-ms :duration-ms track-id :track-id
                                   track-name :name :as track}]
  (let [analysis-data (api-spotify/track-id->analysis track-id)
        winning-section (shuffle
                          (filterv #(is-section-valid-for-ph? duration-ms %)
                                   (:sections analysis-data)))]
    (first winning-section)))

(defn vec-of-track-objs->relative-ph-data [data]
  (vec (pmap (fn [{{duration-ms :duration_ms track-id :id
                    track-name :name :as track} :track}]
               {:track-id track-id
                :duration-ms duration-ms
                :track-name track-name
                :artist-name (-> track :artists first :name)
                :start-section (track-info->get-start-time {:duration-ms duration-ms
                                                            :track-id track-id})}) data)))


(defn init-ph-state-via-playlist-id! [user-id playlist-id song-count]
  (let [song-list (api-spotify/handle-paginated-requests
                    (format (:playlists-list-songs api-spotify/urls)
                            (-> (api-spotify/filter-playlist-by-id
                                  playlist-id
                                  (api-spotify/handle-paginated-requests
                                    (format (:playlists-get api-spotify/urls) user-id)))
                                first :id)))
        ph-data (vec-of-track-objs->relative-ph-data song-list)
        shuffled-data (vec (take song-count (shuffle ph-data)))]
    (reset! cmn-session/power-hour-state {:songs shuffled-data})))

;(def ph-data
;  (init-ph-state-via-playlist-id!
;    "mdrago1026"
;    "75M2u29GVTzqp5q6p51IRC"
;    60))



;(defn do-power-hour []
;  (future (doseq [{:keys [track-name artist-name track-id start-section]} (:songs ph-data)
;          :let [{:keys [start]} start-section
;                start-ms (* 1000 start)]]
;    (info (format "Now Playing: %s by %s (at %s start sec)" track-name artist-name start))
;    (api-spotify/play-song-from-ms
;      "2129f633235a6ec17e1317d165a73eb6eb21d9b1"
;      track-id
;      start-ms)
;    (Thread/sleep 60000))))

;(first (:songs ph-data))
;
;ph-data
