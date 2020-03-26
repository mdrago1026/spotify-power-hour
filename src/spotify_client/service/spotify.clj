(ns spotify-client.service.spotify
  (:require [spotify-client.spotify.api :as api-spotify]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.common.session :as cmn-session]
            [spotify-client.config.general :as cfg-gen]))

(defn spotify-search [search-term]
  (let [char-count (count search-term)]
    (if (< char-count cfg-gen/spotify-search-min-char)
      {:valid? false :msg (format "Query string (q) must be at least %s characters" cfg-gen/spotify-search-min-char)
       :status 400}
      (let [raw-data (api-spotify/search search-term)
            xformed-data (mapv
                           (fn [{:keys [id name artists]}]
                             {:id id
                              :track-name name
                              :artists (-> artists first :name)}) (-> raw-data :tracks :items))]
        {:valid? true :data xformed-data :status 200}))))

(defn spotify-player-queue [track-id]
  (info "TRACK ID: "track-id)
  (let [char-count (count track-id)]
    (if (< char-count cfg-gen/spotify-uri-track-length)
      {:valid? false :msg (format "track_id must be exactly %s characters" cfg-gen/spotify-uri-track-length)
       :status 400}
      (let [resp (api-spotify/queue-song track-id)]
        {:valid? true :data resp :status 200}))))
