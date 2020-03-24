(ns spotify-client.power-hour.main
  (:require [spotify-client.spotify.api :as api-spotify]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]))

(def power-hour-seconds 60)

;; for testing
(def halo-night-songs
  (api-spotify/handle-paginated-requests
    (format (:playlists-list-songs api-spotify/urls)
            (-> (api-spotify/filter-playlist-by-name
                  "HALO nighT"
                  (api-spotify/handle-paginated-requests
                    (format (:playlists-get api-spotify/urls) "mdrago1026")))
                first :id))))


(defn is-section-valid-for-ph? [total-song-ms {:keys [start duration] :as section-obj}]
  (let [start-ms (* 1000 start)
        duration-ms (* 1000 duration)
        ;;end-ms (+ start-ms duration-ms)
        power-hour-duration (* 1000 power-hour-seconds)]
    (if (<= power-hour-duration duration-ms) ;; if this loud section is 60 sec or greater, we know we are good
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
        winning-section (reverse (sort-by :loudness
                                 (filterv #(is-section-valid-for-ph? duration-ms %)
                                          (:sections analysis-data))))]
    (first winning-section)))

(defn vec-of-track-objs->relative-ph-data [data]
  (mapv (fn [{{duration-ms :duration_ms track-id :id
               track-name :name :as track} :track}]
          {:track-id track-id
           :duration-ms duration-ms
           :track-name track-name
           :artist-name (-> track :artists first :name)
           :start-section (track-info->get-start-time {:duration-ms duration-ms
                                                       :track-id track-id})}) data))

(def simple-design (api-spotify/track-id->analysis "2lpcY0lROi0khLsnBCMp1W"))
(def i-am-a-stone (api-spotify/track-id->analysis "4pJ1UhXr5TfRKNDsjOT0Zi"))



(def simple-design-track-data
  (first (filterv #(= "Simple Design" (:track-name %)) (vec-of-track-objs->relative-ph-data halo-night-songs))))

(def i-am-a-stone-track-data
  (first (filterv #(= "I Am a Stone" (:track-name %)) (vec-of-track-objs->relative-ph-data halo-night-songs))))

simple-design-track-data
i-am-a-stone-track-data ;; 346013ms total

(track-info->get-start-time simple-design-track-data)
(track-info->get-start-time i-am-a-stone-track-data)


(-> (sort-by :loudness (-> simple-design :sections)) reverse first)
(-> (sort-by :loudness (-> i-am-a-stone :sections)) reverse)

(is-section-valid-for-ph? 346013 (nth
                                   (-> (sort-by :loudness (-> i-am-a-stone :sections)) reverse)
                                   0))

;;(spit "/tmp/simple_design.json" (json/generate-string simple-design))

;; 254786 ;; simple design length in ms
;; 190.20936 loud start
;; 24.44862 ;; duration

;; simple design track id 2lpcY0lROi0khLsnBCMp1W



(def data-with-start-time
  (vec-of-track-objs->relative-ph-data halo-night-songs))

halo-night-songs

data-with-start-time
