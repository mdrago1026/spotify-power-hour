(ns spotify-client.config.logging
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as s])
  (:import [java.lang.management ManagementFactory]))

(defn output-fn
  [{:keys [vargs_ hostname_ timestamp_ level ?ns-str ?line ?file] :as args}]
  (let [thread-id (.getId (Thread/currentThread))
        thread-name (.getName (Thread/currentThread))
        pid-host (.getName (ManagementFactory/getRuntimeMXBean))
        [pid host] (s/split pid-host #"@")]
    (format "[%s] [%s - (%s)] [%s - %s - %s] %s" @timestamp_ (name level) thread-id
            pid ?ns-str ?line (apply str @vargs_))))

(defn load-logging-config! []
  (timbre/merge-config!
    {:level :info
     :appenders
     {:println {:output-fn output-fn
                :enabled?  true}}
     :timestamp-opts
     {:pattern  "yyyy/MM/dd HH:mm:ss ZZ"
      :timezone (java.util.TimeZone/getTimeZone "America/New_York")
      :locale   (java.util.Locale. "en-US")}}))


