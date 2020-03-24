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
   :playlist-get "https://api.spotify.com/v1/users/%s/playlists"})

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

(defn get-user-playlists [& [user]]
  (let [url (format (:playlist-get urls) user spotify-user)
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Authorization" (str "Bearer "(:access_token @cmn-session/spotify-session) )
                                    "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(get-user-playlists "mdrago1026")
