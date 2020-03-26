(ns spotify-client.service.spotify
  (:require [spotify-client.spotify.api :as api-spotify]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [cheshire.core :as json]
            [spotify-client.common.session :as cmn-session]))

(defn spotify-search [search-term]
  (let [raw-data (api-spotify/search search-term)
        xformed-data (mapv
                       (fn [{:keys [id name artists]}]
                         {:id id
                          :track-name name
                          :artists (-> artists first :name)}) (-> raw-data :tracks :items))]
    xformed-data))

;(spotify-search "far away breaking")
;
;(api-spotify/search "far away breaking")
;
;(try
;  (spotify-search "far away breaking")
;  (catch Exception e
;    (ex-data e)))
