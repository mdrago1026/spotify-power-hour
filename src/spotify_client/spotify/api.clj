(ns spotify-client.spotify.api
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.common.session :as cmn-session]))

(def auth-token (System/getenv "SPOTIFY_CLJ_AUTH_TOKEN"))
(def spotify-user (System/getenv "SPOTIFY_USER"))

(def spotify-uris
  {:track "spotify:track:%s"})

(def urls
  {:token "https://accounts.spotify.com/api/token"
   :playlists-get "https://api.spotify.com/v1/users/%s/playlists?offset=0&limit=20"
   :playlists-list-songs "https://api.spotify.com/v1/playlists/%s/tracks"
   :tracks-analysis "https://api.spotify.com/v1/audio-analysis/%s"
   :oauth-url "https://accounts.spotify.com/authorize?client_id=ff1826b82af24af9b95d0a951a676ab5&response_type=code&redirect_uri=https%3A%2F%2Fhaloof-dev.ngrok.io%2Fspotify%2Fcallback&scope=user-read-private%20user-read-email%20playlist-read-collaborative"
   :player-get-devices "https://api.spotify.com/v1/me/player/devices"
   :player-start-song "https://api.spotify.com/v1/me/player/play?device_id=%s"
   :player-me "https://api.spotify.com/v1/me/player"
   :player-queue "https://api.spotify.com/v1/me/player/queue?uri=%s"})

(defn get-refresh-token []
  (let [url (:token urls)
        {:keys [status body headers] :as resp}
        (client/post url {:form-params {:grant_type "refresh_token"
                                        :refresh_token (:refresh_token @cmn-session/spotify-session)}
                          :headers {"Authorization" (str "Basic " auth-token)
                                    "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;; TODO:: this needs to be cleaned/loopified
(defn wrap-oauth-refresh [http-fn & args]
  (try
    (if args
      (apply http-fn args)
      (http-fn))
    (catch Exception e
      (when-let [exception-data (ex-data e)]
        (when (= 401 (:status exception-data))
          (warn "TOKEN EXPIRED!")
          (let [refresh-token-results (get-refresh-token)]
            (swap! cmn-session/spotify-session assoc :access_token (:access_token refresh-token-results))
            (info refresh-token-results)
            (info "Calling again...")
            (wrap-oauth-refresh http-fn args)))))))

(defn call-oauth-url []
  (let [url (:oauth-url urls)
        {:keys [status body headers] :as resp}
        (client/get url {:redirect-strategy :default
                         :headers {"Content-type" "application/json; charset=utf-8"}})]
    resp))

(defn get-access-token [code]
  (let [url (:token urls)
        {:keys [status body headers] :as resp}
        (client/post url {:form-params {:grant_type "authorization_code"
                                        :code code
                                        :redirect_uri "https://haloof-dev.ngrok.io/spotify/callback"}
                          :headers {"Authorization" (str "Basic " auth-token)
                                    "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(get-access-token)

(defn get-user-playlists* [user & {:keys [url]}]
  (let [url (or url (format (:playlists-get urls) (or user spotify-user)))
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;(defn get-user-playlists [user]
;  (let [all-paged-data (loop [url nil
;                              overall-playlists []]
;                         (let [{:keys [next] :as curr-playlists} (get-user-playlists* user :url url)]
;                           (if next
;                             (recur
;                               next
;                               (conj overall-playlists curr-playlists))
;                             (conj overall-playlists curr-playlists))))
;        combined-data (reduce
;                        (fn [r {:keys [items]}]
;                          (concat r items)) [] all-paged-data)]
;    combined-data))

(defn handle-paginated-requests [initial-url]
  (let [all-paged-data
        (loop [url nil
               overall-data []]
          (let [{:keys [status body headers] :as resp}
                (client/get (or url initial-url)
                            {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                       "Content-type" "application/json; charset=utf-8"}})
                {:keys [next] :as parsed-body} (json/parse-string body true)]
            (if next
              (recur
                next
                (conj overall-data parsed-body))
              (conj overall-data parsed-body))))
        combined-data (reduce
                        (fn [r {:keys [items]}]
                          (concat r items)) [] all-paged-data)]
    combined-data))

(defn filter-playlist-by-name [search-term playlists-vec]
  (let [filtered (filterv (fn [{:keys [name]}] (= (s/lower-case name) (s/lower-case search-term))) playlists-vec)]
    filtered))

(defn filter-playlist-by-id [input-playlist-id playlists-vec]
  (let [filtered (filterv (fn [{:keys [id]}] (= id input-playlist-id)) playlists-vec)]
    filtered))

;;; https://open.spotify.com/playlist/75M2u29GVTzqp5q6p51IRC?si=NQRCUMapQ-S37eMg_PhOYQ
;
;;; https://api.spotify.com/v1/users/mdrago1026/playlists/75M2u29GVTzqp5q6p51IRC?si=NQRCUMapQ-S37eMg_PhOYQ/tracks

(defn plalyist-id->track-list [playlist-id]
  (let [url (format (:playlists-list-songs urls) playlist-id)
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn track-id->analysis [track-id]
  (let [url (format (:tracks-analysis urls) track-id)
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn get-active-devices []
  (let [url (:player-get-devices urls)
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;(wrap-oauth-refresh get-active-devices)
;(get-active-devices)

(defn start-playing [device-id]
  (let [url (:player-me urls)
        body (json/generate-string {:device_ids [device-id]
                                    :play true})
        {:keys [status body headers] :as resp}
        (client/put url {:body body
                         :headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn play-song-from-ms [device-id track-id position-ms]
  (let [url (format (:player-start-song urls) device-id)
        body (json/generate-string {:uris [(format (:track spotify-uris) track-id)]
                                    :position_ms position-ms})
        {:keys [status body headers] :as resp}
        (client/put url {:body body
                         :headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn queue-song [track-id]
  (let [url (format (:player-queue urls) (format (:track spotify-uris) track-id))
        {:keys [status body headers] :as resp}
        (client/post url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(queue-song "2lpcY0lROi0khLsnBCMp1W")

;;(start-playing "2129f633235a6ec17e1317d165a73eb6eb21d9b1")

;(play-song-from-ms
;  "2129f633235a6ec17e1317d165a73eb6eb21d9b1"
;  "2lpcY0lROi0khLsnBCMp1W"
;  (* 1000 190.20936)
;  )

;;(get-active-devices)

;;(track-id->analysis "2lpcY0lROi0khLsnBCMp1W")

;(def my-user-playlists (handle-paginated-requests (format (:playlists-get urls) "mdrago1026")))
;
;(def halo-night-playlist-id (-> (filter-playlist-by-name "HALO nighT" my-user-playlists) first :id))
;(def halo-night-songs (plalyist-id->track-list halo-night-playlist-id))
;
;
;(def halo-night-songs
;  (handle-paginated-requests (format (:playlists-list-songs urls) halo-night-playlist-id)))
;
;;;(count halo-night-songs)
;
;(first halo-night-songs)


;; DRAGON PRO DEVICE ID: 2129f633235a6ec17e1317d165a73eb6eb21d9b1
