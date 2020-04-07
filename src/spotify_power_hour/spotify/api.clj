(ns spotify-power-hour.spotify.api
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-power-hour.common.session :as cmn-session]
            [ring.util.codec :as ring-codec]
            [spotify-power-hour.config.general :as cfg-gen]))

(def auth-token (System/getenv "SPOTIFY_CLJ_AUTH_TOKEN"))
(def spotify-user (System/getenv "SPOTIFY_USER"))

(def spotify-uris
  {:track "spotify:track:%s"})

(def urls
  {:token "https://accounts.spotify.com/api/token"
   :playlists-get "https://api.spotify.com/v1/users/%s/playlists?offset=0&limit=20"
   :playlists-list-songs "https://api.spotify.com/v1/playlists/%s/tracks"
   :tracks-analysis "https://api.spotify.com/v1/audio-analysis/%s"
   :tracks-get "https://api.spotify.com/v1/tracks/%s"
   :player-get-devices "https://api.spotify.com/v1/me/player/devices"
   :player-start-song "https://api.spotify.com/v1/me/player/play?device_id=%s"
   :player-me "https://api.spotify.com/v1/me/player"
   :player-queue "https://api.spotify.com/v1/me/player/queue?uri=%s"
   :search "https://api.spotify.com/v1/search?type=track&limit=10&q=%s"
   :users-me "https://api.spotify.com/v1/me"
   ;; client / state / redir-uri
   :oauth-url "https://accounts.spotify.com/authorize?client_id=%s&state=%s&response_type=code&redirect_uri=%s&scope=user-read-private%%20user-read-email%%20playlist-read-collaborative%%20user-modify-playback-state%%20user-read-playback-state"
   })

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
 ;; (info "ARGS: "args)
  (try
    (if args
      (let [arg-map (nth args 1)
            final-arg-map arg-map                           ;;(assoc arg-map :throw-exceptions false)
            final-args [(first args) final-arg-map]]
        (apply http-fn final-args))
      (http-fn))
    (catch Exception e
      (if-let [exception-data (ex-data e)]
        (if (= 401 (:status exception-data))
          (do
            (warn "TOKEN EXPIRED!")
            (let [refresh-token-results (get-refresh-token)]
              (swap! cmn-session/spotify-session assoc :access_token (:access_token refresh-token-results))
              (info refresh-token-results)
              (info "Calling again...")
              (wrap-oauth-refresh http-fn args)))
          (do
            (warn "Caught generic exception, rethrowing...")
            (let [parsed-body (json/parse-string (:body exception-data) true)
                  {:keys [status message] :as error-context} (:error parsed-body)]

              (info "ERROR BODY: " error-context)
              (throw (ex-info "Spotify Exception" error-context)))))
        (throw e)))))

(defn call-oauth-url []
  (let [url (:oauth-url urls)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:redirect-strategy :default
                                            :headers {"Content-type" "application/json; charset=utf-8"}})]
    resp))

(defn get-access-token [code & [state]]
  (let [url (:token urls)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/post url {:form-params {:grant_type "authorization_code"
                                                           :code code
                                                           :redirect_uri (System/getenv "SPOTIFY_CLIENT_REDIRECT_URI")}
                                             :headers {"Authorization" (str "Basic " auth-token)
                                                       "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(get-access-token)

(defn get-user-playlists* [user & {:keys [url]}]
  (let [url (or url (format (:playlists-get urls) (or user spotify-user)))
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
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
                (wrap-oauth-refresh client/get (or url initial-url)
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
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))
;
;(-> (plalyist-id->track-list "75M2u29GVTzqp5q6p51IRC")
;    :items
;    first
;    :track)

(defn track-id->analysis [track-id]
  (let [url (format (:tracks-analysis urls) track-id)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn get-active-devices []
  (let [url (:player-get-devices urls)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;(wrap-oauth-refresh get-active-devices)
;(get-active-devices)

(defn get-current-player-info []
  (let [url (:player-me urls)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(get-current-player-info)

(defn play-song-from-ms [device-id track-id position-ms]
  (let [url (format (:player-start-song urls) device-id)
        body (json/generate-string {:uris [(format (:track spotify-uris) track-id)]
                                    :position_ms position-ms})
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/put url {:body body
                                            :headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn queue-song [track-id]
  (let [url (format (:player-queue urls) (format (:track spotify-uris) track-id))
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/post url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                       "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn search [search-term]
  (let [url (format (:search urls) (ring-codec/url-encode search-term))
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get
                            url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                           "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

;;(search "fAr Away")

(defn get-track [track-id]
  (let [url (format (:tracks-get urls) track-id)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))

(defn my-profile []
  (let [url (:users-me urls)
        {:keys [status body headers] :as resp}
        (wrap-oauth-refresh client/get url {:headers {"Authorization" (str "Bearer " (:access_token @cmn-session/spotify-session))
                                                      "Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    parsed-body))


;;; UI SPECIFIC STUFF

(defn client-id->oauth-url [client-id redir-uri session-id]
  (format (:oauth-url urls) client-id session-id redir-uri))

;; #"(?<!\/)\/(?!\/)"
;; (?<!\/) Assert that the previous character isn't a /
;; \/      Match a literal /
;; (?!\/)  Assert that the next character isn't a /
(defn redir-url-and-session-id->verification-url [redir-url session-id]
  (let [[base-uri & rest] (s/split redir-url #"(?<!\/)\/(?!\/)")
        final-verify-url (str base-uri "/" (format cfg-gen/spotify-verify-uri session-id))]
    final-verify-url))

(defn verify-authentication
  "Returns a map of {:valid bool :token {}} if valid"
  [redir-url session-id]
  (let [url (redir-url-and-session-id->verification-url redir-url session-id)
        _ (info "ATTEMPTING TO VALIDATE SESSION ID with URL: "url)
        {:keys [status body headers] :as resp}
        (client/get url {:headers {"Content-type" "application/json; charset=utf-8"}})
        parsed-body (json/parse-string body true)]
    (:data parsed-body)))

;(verify-authentication
;  "https://haloof-dev.ngrok.io/spotify/callback"
;  "9b0e725f-1de1-4682-a900-9ef8b53cf62d")


;;(track-id->analysis "2lpcY0lROi0khLsnBCMp1W")
