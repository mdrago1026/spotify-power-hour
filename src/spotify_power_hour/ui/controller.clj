(ns spotify-power-hour.ui.controller
  (:require [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.common :as ui-cmn]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.ui.components.common :as cmn-comp]
            [seesaw.mig :as mig]
            [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [spotify-power-hour.spotify.api :as api-spotify]
            [clojure.java.browse :as browse]
            [spotify-power-hour.controller.spotify :as ctrl-spotify]
            [spotify-power-hour.config.general :as cfg-gen])
  (:import (java.util UUID)))

;;; DEBUG

(defn menu-debug-playlist-selection-handler [e]
  (swap! ui-cmn/app-state assoc
         :scene cmn-ui/ui-scene-power-hour-main))

;;; LOGOUT

(defn menu-account-logout-handler [e]
  (info "Logging out!")
  (reset! cmn-session/local-session-id (UUID/randomUUID))
  (reset! cmn-session/spotify-session nil)
  (cmn-comp/update-shared-user-info-component cmn-comp/user-info-not-logged-in)
  (swap! ui-cmn/app-state assoc
         :authenticated? false
         :token nil
         :user-info nil
         :scene cmn-ui/ui-scene-login
         :status (get-in cmn-ui/ui-states [:logout :logout])))


;; LOGIN

(defn verify-login [redir-url]
  (try
    {:valid? true
     :response (ctrl-spotify/attempt-to-validate-oauth redir-url @cmn-session/local-session-id)}
    (catch Exception e
      {:valid? false
       :response (ex-data e)})))

(defn handle-successful-login [token]
  (info "Successfully authenticated!")
  (reset! cmn-session/spotify-session token)
  (let [{:keys [id] :as user-info} (api-spotify/my-profile)
        new-text (format "Logged in as: %s" id)]
    (cmn-comp/update-shared-user-info-component new-text)
    (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :successfully-authed])
           :authenticated? true
           :user-info user-info
           :scene cmn-ui/ui-scene-power-hour-main)))

(defn handle-client-id-submit [e]
  (future
    (let [client-id (text (select (to-root e) [:#login-client-id]))
          redir-uri (text (select (to-root e) [:#login-redirect-uri]))
          oauth-url (api-spotify/client-id->oauth-url client-id redir-uri @cmn-session/local-session-id)]
      (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]))
      (info "OAUTH URL: " oauth-url)
      (browse/browse-url oauth-url)
      (let [{:keys [valid? response]} (verify-login redir-uri)]
        (if valid?
          (handle-successful-login (:token response))
          (do
            (info "Failed to retrieve user details with error: " response)
            (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :failed-to-auth])
                   :authenticated? false)))))))

;; CALLBACKS

(defn get-spotify-plalysts-for-user-id
  "Given a spotify user-id, returns a sorted (lexa, asc) vec of maps with :name/:id for each playlist"
  [user-id]
  (let [spotify-api-url (format (:playlists-get api-spotify/urls) user-id)
        user-playlists (vec (api-spotify/handle-paginated-requests
                              spotify-api-url))
        mapped-playlists (sort-by :name (mapv #(select-keys % [:name :id]) user-playlists))]
    mapped-playlists))

(defn init-ph-main-scene [ui]
  (invoke-later
    (info "Power Hour Main Scene INIT Callback FUNC!")
    (config! (select ui [:#ph-main-select-playlist]) :enabled? false)
    (config! (select ui [:#ph-main-select-spinner]) :visible? false)
    (let [{:keys [id] :as user-info} (:user-info @cmn-ui/app-state)
          sorted-playlists (get-spotify-plalysts-for-user-id id)]
      (swap! cmn-ui/app-state assoc-in [:spotify :playlists] sorted-playlists)
      (config! (select ui [:#ph-main-select-playlist]) :model sorted-playlists)
      (config! (select ui [:#ph-main-select-playlist]) :enabled? true)
      (config! (select ui [:#ph-main-select-spinner]) :visible? false)
      (selection! (select ui [:#ph-main-select-playlist]) (first (get-in @cmn-ui/app-state [:spotify :playlists])))
      sorted-playlists)))

;;(get-spotify-plalysts-for-user-id  "mdrago1026")


;;;;; POWER HOUR SONG ALGO

(def power-hour-seconds 60)

(defn is-section-valid-for-ph? [total-song-ms {:keys [start duration] :as section-obj}]
  (let [start-ms (* 1000 start)
        duration-ms (* 1000 duration)
        power-hour-duration (* 1000 power-hour-seconds)]
    (if (<= power-hour-duration duration-ms)                ;; if this loud section is 60 sec or greater, we know we are good
      true
      (if (<= power-hour-duration (- total-song-ms start-ms)) ;; if we have at least 60 seconds from the start of loud to end of song
        true
        false))))

(defn track-info->get-start-time [{duration-ms :duration-ms track-id :track-id
                                   track-name :name :as track}]
  (let [analysis-data (api-spotify/track-id->analysis track-id)
        winning-section (shuffle
                          (filterv #(is-section-valid-for-ph? duration-ms %)
                                   (:sections analysis-data)))]
    (first winning-section)))

(defn vec-of-track-objs->relative-ph-data [data]
  (vec (map (fn [{{duration-ms :duration_ms track-id :id
                   track-name :name
                   {images :images} :album :as track} :track}]
              (let [{:keys [height width url] :as image} (nth images 1 nil)] ;; 0 = 640, 1 = 300, 2 = 64
                {:track-id track-id
                 :duration-ms duration-ms
                 :track-name track-name
                 :artist-name (-> track :artists first :name)
                 :image-url url
                 ;:start-section (track-info->get-start-time {:duration-ms duration-ms
                 ;                                            :track-id track-id})
                 })) data)))

(defn add-start-time-to-relative-ph-data [data number-of-elements]
  (let [data-to-update (vec (take number-of-elements data))
        data-not-updated (vec (drop number-of-elements data))
        _ (swap! cmn-ui/app-state assoc-in [:spotify :song-data-to-load] (count data-to-update))
        updated-data (vec (map (fn [{:keys [track-id duration-ms] :as c}]
                                 (swap! cmn-ui/app-state update-in [:spotify :song-data-loaded] inc)
                                 (assoc c :start-section
                                          (track-info->get-start-time {:duration-ms duration-ms
                                                                       :track-id track-id}))) data-to-update))
        final-data (vec (concat updated-data data-not-updated))]
    (info "BYE!")
    final-data))

(defn spotify-playlist-id->songs
  [playlist-id]
  (let [spotify-api-url (format (:playlists-list-songs api-spotify/urls) playlist-id)
        song-list (vec (api-spotify/handle-paginated-requests
                         spotify-api-url))
        formatted-song-list (vec-of-track-objs->relative-ph-data song-list)]
    formatted-song-list))

;;(spotify-playlist-id->songs "75M2u29GVTzqp5q6p51IRC")
;;(spotify-playlist-id->songs "378rfNHckTTvsQazLOwsbv")

(defn util-stop-loading-song-count [e]
  (config! (select (to-root e) [:#ph-main-select-playlist]) :enabled? true)
  (config! (select (to-root e) [:#ph-main-select-spinner]) :visible? false)
  (config! (select (to-root e) [:#ph-main-start-ph-btn]) :enabled? true)
  (config! (select (to-root e) [:#ph-main-select-song-count]) :enabled? true))

(defn ph-select-playlist [e {:keys [id name] :as selection}]
  (future
    (config! (select (to-root e) [:#ph-main-select-playlist]) :enabled? false)
    (config! (select (to-root e) [:#ph-main-select-spinner]) :visible? true)
    (config! (select (to-root e) [:#ph-main-start-ph-btn]) :enabled? false)
    (config! (select (to-root e) [:#ph-main-select-song-count]) :enabled? false)
    (info "Playlist selected: " selection)
    (let [song-list (spotify-playlist-id->songs id)
          songs-at-least-a-minute (filterv (fn [{:keys [duration-ms]}]
                                             (<= cfg-gen/min-song-length-ms duration-ms)) song-list)
          songs-at-least-a-minute-count (count songs-at-least-a-minute)
          song-count (count song-list)
          final-text (str cmn-comp/ph-default-playlist-count-text song-count)
          final-text-over-60 (str cmn-comp/ph-default-playlist-over-60-count-text songs-at-least-a-minute-count)
          min-song-count (last cmn-ui/ui-ph-song-count-defaults)
          max-song-count (first cmn-ui/ui-ph-song-count-defaults)
          option-set (set cmn-ui/ui-ph-song-count-defaults)
          below-min? (< song-count min-song-count)
          below-max? (< song-count max-song-count)]
      (swap! cmn-ui/app-state assoc-in [:spotify :selected-playlist-songs] song-list)
      (swap! cmn-ui/app-state assoc-in [:spotify :selected-playlist-song-count] song-count)
      ;; update model to only have valid song count choices
      (if below-max?                                        ;; if we are below the max, ie: 50, 42, 31,
        (let [filtered-option-set (filter #(< % song-count) cmn-ui/ui-ph-song-count-defaults)
              song-count-already-option? (contains? option-set song-count) ;; don't want it there twice
              final-vec (if (and
                              ;; if the selected playlist song count is not already a default
                              (not song-count-already-option?)
                              ;; if we filtered anything out at all, add the actual song count as the final
                              (not= (count cmn-ui/ui-ph-song-count-defaults) (count filtered-option-set)))
                          (conj filtered-option-set song-count)
                          filtered-option-set)]
          (config! (select (to-root e) [:#ph-main-select-song-count]) :model final-vec)
          (selection! (select (to-root e) [:#ph-main-select-song-count]) (first final-vec)))
        ;; otherwise, reset to the defaults
        (config! (select (to-root e) [:#ph-main-select-song-count]) :model cmn-ui/ui-ph-song-count-defaults))

      (config! (select (to-root e) [:#ph-main-selected-playlist-song-count-label]) :text final-text)
      (config! (select (to-root e) [:#ph-main-selected-playlist-over-60-song-count-label]) :text final-text-over-60)
      (info "Song count: " song-count)

      (if below-min?
        (do
          (config! (select (to-root e) [:#ph-main-not-enough-songs-error])
                   :text (format cmn-comp/ph-selected-playlist-not-enough-songs min-song-count)
                   :visible? true)
          (config! (select (to-root e) [:#ph-main-start-ph-btn]) :visible? false))
        ;; if we have >= 60 songs in the selected playlist
        (do
          (selection! (select (to-root e) [:#ph-main-select-song-count]) max-song-count)
          (config! (select (to-root e) [:#ph-main-not-enough-songs-error]) :visible? false)
          (config! (select (to-root e) [:#ph-main-select-song-count]) :enabled? true)))

      (util-stop-loading-song-count e))))


;; (config! (select ui [:#ph-main-select-song-count]) :enabled? true)

(defn ph-handle-song-count-select [e selection]
  (info "Selected song count: " selection)
  (config! (select (to-root e) [:#ph-main-start-ph-btn]) :visible? true))

(defn handle-start-ph [e]
  (swap! cmn-ui/app-state assoc
         :status (get-in cmn-ui/ui-states [:ph :retrieving-song-starts])
         :scene cmn-ui/ui-scene-power-hour-loading))

(defn song-data-analysis-worker [preload-count]
  (let [current-song-data (get-in @cmn-ui/app-state [:spotify :selected-playlist-songs])
        current-song-data-count (count current-song-data)
        remaining-count (- current-song-data-count preload-count)
        song-range (range preload-count current-song-data-count)]
    (info (format "[WORKER] Starting background song-data gatherer. Total songs in selected playlist: (%s). Pre-fetch count: (%s). Songs remaining: (%s)"
                  current-song-data-count preload-count remaining-count))
    (doseq [i song-range
            :let [{:keys [track-id duration-ms] :as c} (nth current-song-data i)]]
      (info (format "[WORKER] (%s/%s) Starting to process song: %s" i (dec current-song-data-count) c))
      (let [song-analysis (track-info->get-start-time {:duration-ms duration-ms
                                                       :track-id track-id})
            final-data (assoc c :start-section song-analysis)]
        (swap! cmn-ui/app-state assoc-in [:spotify :selected-playlist-songs i] final-data)))
    (info "[WORKER] Background job complete!")))

(defn init-ph-load [mf]
  (future
    (info "Init PH Start callback")
    (swap! cmn-ui/app-state assoc-in [:spotify :song-data-loaded] 0)
    (let [current-song-data (get-in @cmn-ui/app-state [:spotify :selected-playlist-songs])
          add-start-times (add-start-time-to-relative-ph-data current-song-data
                                                              cfg-gen/song-count-to-preload)]
      (info "Done getting song data")
      (future (song-data-analysis-worker cfg-gen/song-count-to-preload))
      (swap! cmn-ui/app-state assoc-in [:spotify :selected-playlist-songs] add-start-times)
      (swap! cmn-ui/app-state assoc
             :status (get-in cmn-ui/ui-states [:ph :ready-to-start])
             :scene ui-cmn/ui-scene-power-hour-ctrl))))

(defn init-ph-ctrl [mf]
  (info "Init PH CTRL!")
  (future

    )
  )

;;(add-start-time-to-relative-ph-data (get-in @cmn-ui/app-state [:spotify :selected-playlist-songs]) 10)


;; Order of operations:
;; 1. load song data for all songs in playlist
;; 2. filter out songs < 60 second
;; 3. users chooses how many songs
;; 4. at random, update the playlist state to have that many songs
;; 5. start power hour
;; 6. load the first 5
;; 7. start bg worker
