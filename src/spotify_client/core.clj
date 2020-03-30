(ns spotify-client.core
  (:gen-class)
  (:require [spotify-client.config.logging :as cfg-log]
            [taoensso.timbre :as timbre]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-params
                                          wrap-json-body]]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.spotify.api :as api-spotify]
            [spotify-client.common.session :as cmn-session]
            [spotify-client.config.general :as cfg-gen]
            [spotify-client.service.spotify :as service-spotify])
  (:use [org.httpkit.server]))

(cfg-log/load-logging-config!)


;; General STATE flow
;; User submits client_id from UI --> auth server
;; Auth server calls handle-spotify-callback
;; if successful auth, update in-mem state that tracks {state -> 1}
;; expose an endpoint that queries this state map

(defn handle-spotify-callback [req]
  (info "SPOTIFY CALLBACK: " req)
  (let [code (get-in req [:params "code"])
        state (get-in req [:params "state"])
        token-resp (api-spotify/get-access-token code state)]
    (info "TOKEN RESP: " token-resp)
    (when (:access_token token-resp)
      (reset! cmn-session/spotify-session token-resp)
      (when state
        (swap! cmn-session/session-id-cache assoc state 1)))
    {:status 200
     :body "Successfully authenticated"}))

(defn wrap-json-body-str [handler]
  (fn [request]
    (handler (assoc request :json-body (try
                                         (json/parse-string (:body request) true)
                                         (catch Exception e))))))

(defn handle-spotify-search [req]
  (let [query-string (get-in req [:params "q"])
        {:keys [valid? status data msg]} (service-spotify/spotify-search query-string)]
    (if valid?
      {:status status
       :headers {"Content-Type" "application/json"}
       :body {:data data}}
      {:status status
       :headers {"Content-Type" "application/json"}
       :body {:msg msg}})))

(defn handle-spotify-player-queue [req]
  (let [query-string (get-in req [:params "track_id"])
        {:keys [valid? status data msg]} (service-spotify/spotify-player-queue query-string)]
    (if valid?
      {:status status
       :headers {"Content-Type" "application/json"}
       :body {:data data}}
      {:status status
       :headers {"Content-Type" "application/json"}
       :body {:msg msg}})))

(defn catch-spotify-exceptions [handler-fn req]
  (try
    (info "Incoming request: " req)
    (handler-fn req)
    (catch Exception e
      (if-let [{:keys [status message] :as error-data} (ex-data e)]
        (do
          (warn "ERROR: " error-data)
          {:status status
           :body {:msg message}})
        {:status 500}))))

(defn handle-spotify-player-current [req]
  (let [{:keys [valid? status data msg]} (service-spotify/spotify-player-current)]
    {:status status
     :headers {"Content-Type" "application/json"}
     :body {:data data}}))

(defn handle-verify-ui [req]
  (let [state (get-in req [:params "state"])
        is-valid? (get @cmn-session/session-id-cache state)
        base {:status 200
              :headers {"Content-Type" "application/json"}}]
    (if is-valid?
      (assoc base :body {:data {:valid true}})
      (assoc base :body {:data {:valid false}}))))


(defroutes
  app-routes
  (GET "/spotify/callback" req (catch-spotify-exceptions handle-spotify-callback req))
  (GET "/spotify/callback/verify-ui" req (handle-verify-ui req))
  (GET "/spotify/search" req (catch-spotify-exceptions handle-spotify-search req))
  (GET "/spotify/player/current" req (catch-spotify-exceptions handle-spotify-player-current req))
  (POST "/spotify/player/queue" req (catch-spotify-exceptions handle-spotify-player-queue req))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-params
      ;;wrap-keyword-params
      wrap-json-response
      wrap-json-body-str))

(defn preserve-raw-body
  "This is needed because there is no way to get the raw body of a
  application/x-www-form-urlencoded request as ring automatically converts
  it to a map. Slack HMAC verification requires the raw body."
  [req]
  (if (:body req)
    (let [body (slurp (:body req))
          final-req (assoc req :raw-body body :body body)]
      (app final-req))
    (app req)))

(defn -main [& [ip port]]
  (when-let [env-level (System/getenv "SLACKBOT_LOGGING")]
    (warn "Setting log level to: " env-level)
    (timbre/set-level! (keyword env-level)))
  (info "Starting Spotify Client!")
  (run-server app {:port (Integer. port) :ip ip}))

;(-main "localhost" 8080)
