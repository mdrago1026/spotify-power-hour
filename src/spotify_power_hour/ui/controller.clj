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
            [spotify-power-hour.controller.spotify :as ctrl-spotify]))

;;; LOGOUT

(defn menu-account-logout-handler [e]
  (info "Logging out!")
  (reset! cmn-session/spotify-session {})
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
     :response (ctrl-spotify/attempt-to-validate-oauth redir-url cmn-session/local-session-id)}
    (catch Exception e
      {:valid? false
       :response (ex-data e)})))

(defn handle-client-id-submit [e]
  (future
    (let [client-id (text (select (to-root e) [:#login-client-id]))
          redir-uri (text (select (to-root e) [:#login-redirect-uri]))
          oauth-url (api-spotify/client-id->oauth-url client-id redir-uri cmn-session/local-session-id)]
      (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]))
      (info "OAUTH URL: " oauth-url)
      (browse/browse-url oauth-url)
      (let [{:keys [valid? response]} (verify-login redir-uri)]
        (if valid?
          (do
            (info "Successfully authenticated!")
            (reset! cmn-session/spotify-session (:token response))
            (let [{:keys [id] :as user-info} (api-spotify/my-profile)
                  new-text (format "Logged in as: %s" id)]
              (cmn-comp/update-shared-user-info-component new-text)
              (swap! cmn-ui/app-state assoc :status (get-in cmn-ui/ui-states [:login :successfully-authed])
                     :authenticated? true
                     :user-info user-info
                     :scene cmn-ui/ui-scene-power-hour-main)))
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
  (vec (pmap (fn [{{duration-ms :duration_ms track-id :id
                    track-name :name :as track} :track}]
               {:track-id track-id
                :duration-ms duration-ms
                :track-name track-name
                :artist-name (-> track :artists first :name)
                ;:start-section (track-info->get-start-time {:duration-ms duration-ms
                ;                                            :track-id track-id})
                }) data)))

(defn spotify-playlist-id->songs
  [playlist-id]
  (let [spotify-api-url (format (:playlists-list-songs api-spotify/urls) playlist-id)
        song-list (vec (api-spotify/handle-paginated-requests
                              spotify-api-url))
        formatted-song-list (vec-of-track-objs->relative-ph-data song-list)]
    formatted-song-list))

;;(spotify-playlist-id->songs "75M2u29GVTzqp5q6p51IRC")

(defn ph-select-playlist [e {:keys [id name] :as selection}]
  (info "SELECTION2: "selection)
  (invoke-later
    (info "Playlist selected: "selection)
    (let [song-list (spotify-playlist-id->songs id)
          song-count (count song-list)
          final-text (str cmn-comp/ph-default-playlist-count-text song-count)]

      ;;; LEFT OFF:
      ;;; Here. ngrok went down lol

      (config! (select (to-root e) [:#ph-main-selected-playlist-song-count-label]) :text final-text)
      (info "Song count: "song-count)
      )
    )
  )




;; (config! (select ui [:#ph-main-select-song-count]) :enabled? true)
