(ns spotify-client.core
  (:gen-class)
  (:require [spotify-client.config.logging :as cfg-log]))

(cfg-log/load-logging-config!)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
