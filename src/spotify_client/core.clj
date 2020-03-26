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

(defn handle-spotify-callback [req]
  (info "SPOTIFY CALLBACK: " req)
  (let [code ((:params req) "code")
        _ (info "CODE: "code)
        token-resp (api-spotify/get-access-token code)]
    (info "TOKEN RESP: " token-resp)
    (when (:access_token token-resp)
      (reset! cmn-session/spotify-session token-resp))
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
    (handler-fn req)
    (catch Exception e
      (if-let [{:keys [status message] :as error-data} (ex-data e)]
        (do
          (warn "ERROR: "error-data)
          {:status status
           :body {:msg message}})
        {:status 500}))))

(defroutes
  app-routes
    (GET "/spotify/callback" req (catch-spotify-exceptions handle-spotify-callback req))
    (GET "/spotify/search" req (catch-spotify-exceptions handle-spotify-search req))
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
  (run-server app {:port (Integer. port) :ip ip}))

;(-main "localhost" 8080)
