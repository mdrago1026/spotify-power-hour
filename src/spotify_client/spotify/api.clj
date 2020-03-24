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
   :playlist-get "https://api.spotify.com/v1/users/%s/playlists?offset=0&limit=20"
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
  (let [url (or url (format (:playlist-get urls) (or user spotify-user)))
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                   "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn get-user-playlists [user]
  (let [all-paged-data (loop [url nil
                              overall-playlists []]
                         (let [{:keys [next] :as curr-playlists} (get-user-playlists* user :url url)]
                           (if next
                             (recur
                               next
                               (conj overall-playlists curr-playlists))
                             (conj overall-playlists curr-playlists))))
        combined-data (reduce
                        (fn [r {:keys [items]}]
                          (concat r items)) [] all-paged-data)]
    combined-data))

(defn filter-playlist-by-name [search-term playlists-obj]
  (let [{:keys [href items] :as playlists-obj} playlists-obj
        filtered (filterv (fn [{:keys [name]}] (= (s/lower-case name) (s/lower-case search-term))) items)]
    (assoc playlists-obj :items filtered)))

;(mapv :name (:items (get-user-playlists "mdrago1026")))
;;;
;(filter-playlist-by-name "HaLo NIGHT" (get-user-playlists "mdrago1026"))
;
;;; left off: why are some of my playlists missing?
;
;
;;; https://open.spotify.com/playlist/75M2u29GVTzqp5q6p51IRC?si=NQRCUMapQ-S37eMg_PhOYQ
;
;;; https://api.spotify.com/v1/users/mdrago1026/playlists/75M2u29GVTzqp5q6p51IRC?si=NQRCUMapQ-S37eMg_PhOYQ/tracks
;
;
;(get-user-playlists* "mdrago1026" :url "https://api.spotify.com/v1/users/mdrago1026/playlists?offset=20&limit=20#")
;
;(def recur-future (future (get-user-playlists "mdrago1026")))

