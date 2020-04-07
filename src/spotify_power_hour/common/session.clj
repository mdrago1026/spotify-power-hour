(ns spotify-power-hour.common.session
  (:import (java.util UUID)))

(def spotify-session (atom {}))

(def power-hour-state (atom {}))

;; Map of below `local-session-id` to 1.
;; Being in the map means you're all set
(def session-id-cache (atom {}))

;; Used for UI to call OAUTH server
(def local-session-id (UUID/randomUUID))

;;@session-id-cache
