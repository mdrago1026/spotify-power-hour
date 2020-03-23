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
            [spotify-client.spotify.api :as api-spotify])
  (:use [org.httpkit.server]))

(cfg-log/load-logging-config!)

(defn handle-spotify-callback [req]
  (info "SPOTIFY CALLBACK: "req)
  (let [code (-> req :query-string :code)
        token-resp (api-spotify/get-access-token code)]
    (info "CODE: "code)
    (info "TOKEN RESP: "token-resp)
    )
  {:status 200})

(defn wrap-json-body-str [handler]
  (fn [request]
    (handler (assoc request :json-body (try
                                         (json/parse-string (:body request) true)
                                         (catch Exception e))))))

(defroutes
  app-routes
  (GET "/spotify/callback" req (handle-spotify-callback req))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-json-response
      wrap-json-body-str))

(def app app-routes)



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
  (run-server preserve-raw-body {:port (Integer. port) :ip ip}))

;;(-main "localhost" 8080)
