(ns spotify-power-hour.controller.spotify
  (:require [spotify-power-hour.spotify.api :as api-spotify]
            [again.core :as again]
            [spotify-power-hour.common.retry :as cmn-retry]))

(defn attempt-to-validate-oauth [redir-url session-id]
  (again/with-retries
    cmn-retry/exponential-backoff-strategy
    (api-spotify/verify-authentication redir-url session-id)))

;;(attempt-to-validate-oauth "3fe481ee-bdcc-46e2-b7d6-852711b43b06")
