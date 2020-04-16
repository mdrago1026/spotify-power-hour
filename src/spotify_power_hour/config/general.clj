(ns spotify-power-hour.config.general)

(def spotify-search-min-char 3)
(def spotify-uri-track-length 22)
(def max-power-hour-length-min 60)

(def spotify-verify-uri "spotify/callback/verify-ui?state=%s")

;; The number of songs that should have their data pre-loaded when starting a power hour
(def song-count-to-preload 5)

;; We will filter out songs less than this to ensure each song is at least a minute
(def min-song-length-ms 60000)
