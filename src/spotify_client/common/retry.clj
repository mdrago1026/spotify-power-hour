(ns spotify-client.common.retry
  (:require [again.core :as again]))

(def exponential-backoff-strategy
  (again/max-duration
    30000
    (again/max-retries
      10
      (again/randomize-strategy
        0.5
        (again/multiplicative-strategy 500 1.5)))))
