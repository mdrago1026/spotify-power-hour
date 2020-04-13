(ns spotify-power-hour.common.retry
  (:require [again.core :as again]))

(def exponential-backoff-strategy
  (again/max-duration
    10000
    (again/max-retries
      10
      (again/randomize-strategy
        0.5
        (again/multiplicative-strategy 1000 1.5)))))
