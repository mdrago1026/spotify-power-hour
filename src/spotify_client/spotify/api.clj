(ns spotify-client.spotify.api
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]))

(def auth-token (System/getenv "SPOTIFY_CLJ_AUTH_TOKEN"))

(def urls
  {:token "https://accounts.spotify.com/api/token"})

(defn get-access-token []
  (let [url (:token urls)
        {:keys [status body headers] :as resp}
        (client/post url {:form-params {:grant_type "client_credentials"}
                          :headers {"Authorization" (str "Basic " auth-token)
                                    "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;; Looks like
;{:access_token "blahhhhh",
; :token_type "Bearer",
; :expires_in 3600,
; :scope ""}

;;(get-access-token)
