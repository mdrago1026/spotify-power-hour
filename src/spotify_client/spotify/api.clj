(ns spotify-client.spotify.api
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.common.session :as cmn-session]))

(def auth-token (System/getenv "SPOTIFY_CLJ_AUTH_TOKEN"))
(def spotify-user (System/getenv "SPOTIFY_USER"))

(def urls
  {:token "https://accounts.spotify.com/api/token"
   :playlists-get "https://api.spotify.com/v1/users/%s/playlists?offset=0&limit=20"
   :playlists-list-songs "https://api.spotify.com/v1/playlists/%s/tracks"
   :oauth-url "https://accounts.spotify.com/authorize?client_id=ff1826b82af24af9b95d0a951a676ab5&response_type=code&redirect_uri=https%3A%2F%2Fhaloof-dev.ngrok.io%2Fspotify%2Fcallback&scope=user-read-private%20user-read-email%20playlist-read-collaborative"})

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


(def my-user-playlists (handle-paginated-requests (format (:playlists-get urls) "mdrago1026")))

(def halo-night-playlist-id (-> (filter-playlist-by-name "HALO nighT" my-user-playlists) first :id))
(def halo-night-songs (plalyist-id->track-list halo-night-playlist-id))


(def halo-night-songs
  (handle-paginated-requests (format (:playlists-list-songs urls) halo-night-playlist-id)))

;;(count halo-night-songs)


