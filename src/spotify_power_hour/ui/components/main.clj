(ns spotify-power-hour.ui.components.main
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [clojure.java.io :as io]
            [spotify-power-hour.ui.config :as cfg-ui]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.components.login :as ui-login]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.ui.components.common :as cmn-ui-comp]
            [spotify-power-hour.ui.components.menubar :as ui-menu]
            [spotify-power-hour.ui.components.power-hour.main :as ui-ph]
            [spotify-power-hour.ui.components.power-hour.loading :as ui-ph-load]
            [spotify-power-hour.ui.components.power-hour.controller :as ui-ph-ctrl]
            [spotify-power-hour.ui.controller :as ui-ctrl]
            [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.config.logging :as cfg-log])
  (:import (javax.swing UIManager)
           (com.bulenkov.darcula DarculaLaf)))

(defn get-main-frame [content]
  (let [mf (frame :title cfg-ui/app-name
                  :on-close :dispose
                  :content content
                  :menubar ui-menu/menu-bar
                  :width cfg-ui/ui-width
                  :height cfg-ui/ui-height
                  ;;    :listen [:component-hidden #_hide-frame-cleanup]
                  ;;:resizable? true
                  :id :main-frame)]
    (listen (select mf [:#login-client-id]) :key-pressed ui-login/handle-keys)
    mf))

(defn state-watcher [ui]
  (add-watch
    cmn-ui/app-state :cmn-ui/app-state
    (fn [_ _ old new-state]
      (cond
        (= cmn-ui/ui-scene-login (:scene new-state))
        (cond (= (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]) (new-state :status))
              (invoke-later
                (info "UI STATE: AUTHENTICATING")
                (config! (select ui [:#login-failed-text]) :visible? false)
                (config! (select ui [:#login-success-text]) :visible? false)
                (config! (select ui [:#login-button]) :enabled? false)
                (config! (select ui [:#login-spinner]) :visible? true)
                (config! (select ui [:.login-form]) :enabled? false))

              (= (get-in cmn-ui/ui-states [:login :successfully-authed]) (new-state :status))
              (invoke-later
                (info "UI STATE: SUCCESSFULLY AUTHED")
                (config! (select ui [:#login-button]) :enabled? false)
                (config! (select ui [:#login-spinner]) :visible? false)
                (config! (select ui [:.login-form]) :enabled? false)
                (config! (select ui [:#login-failed-text]) :visible? false)
                (config! (select ui [:#login-success-text]) :visible? true))

              (= (get-in cmn-ui/ui-states [:login :failed-to-auth]) (new-state :status))
              (invoke-later
                (info "UI STATE: LOGIN FAILED")
                (config! (select ui [:#login-button]) :enabled? true)
                (config! (select ui [:#login-spinner]) :visible? false)
                (config! (select ui [:.login-form]) :enabled? true)
                (config! (select ui [:#login-failed-text]) :visible? true)
                (config! (select ui [:#login-success-text]) :visible? false))

              (= (get-in cmn-ui/ui-states [:logout :logout]) (new-state :status))
              (invoke-later
                (info "UI STATE: LOGOUT")
                (config! (select ui [:#login-button]) :enabled? true)
                (config! (select ui [:#login-spinner]) :visible? false)
                (config! (select ui [:.login-form]) :enabled? true)
                (config! (select ui [:#login-success-text]) :visible? false)

                (config! (select ui [:.top-info-logged-in-text])
                         :text cmn-ui-comp/user-info-not-logged-in))
              :else
              (ui-ctrl/menu-account-logout-handler nil))))))

(defn percent-watcher [ui]
  (add-watch
    cmn-ui/app-state :percent-watcher
    (fn [_ _ old new-state]
      (not= (get-in old [:spotify :song-data-loaded]) (get-in old [:spotify :loading-percent]))
      (invoke-later
        (let [new-formatted-val (* 100 (/ (double (get-in old [:spotify :song-data-loaded]))
                                          (double (get-in new-state [:spotify :song-data-to-load]))))]
          (config! (select (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-loading) [:#ph-loading-progress-bar])
                   :value new-formatted-val))))))

(defn scene-watcher [ui]
  (add-watch
    cmn-ui/app-state :scene-watcher
    (fn [_ _ old-state new-state]
      (when (:scene new-state)
        (cond
          (not= (:scene old-state) (:scene new-state))
          (invoke-later
            (info (format "SCENE STATE CHANGE: Old: (%s), New: (%s)"
                          (:scene old-state) (:scene new-state)))
            (let [callback (condp = (:scene new-state)
                             cmn-ui/ui-scene-power-hour-main ui-ctrl/init-ph-main-scene
                             cmn-ui/ui-scene-power-hour-loading ui-ctrl/init-ph-load
                             cmn-ui/ui-scene-power-hour-ctrl ui-ctrl/init-ph-ctrl
                             nil)
                  new-scene (condp = (:scene new-state)
                              cmn-ui/ui-scene-power-hour-main (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-main)
                              cmn-ui/ui-scene-login (cmn-ui/get-panel cmn-ui/ui-scene-login)
                              cmn-ui/ui-scene-power-hour-loading (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-loading)
                              cmn-ui/ui-scene-power-hour-ctrl (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-ctrl)
                              (do
                                (info "Unknown scene: " (:scene new-state))
                                nil))]
              (when new-scene
                (config! (select ui [:#main-frame]) :content new-scene)
                (config! (select ui [:#main-frame])
                         :title (str cfg-ui/app-name " : " (name (:scene new-state))))
                (when callback (callback ui))))))))))

(defn
  ui
  []
  (let [darcula-laf (DarculaLaf.)
        _ (cfg-log/load-logging-config!)
        _ (UIManager/setLookAndFeel darcula-laf)
        login-panel (ui-login/get-login-panel-wrapper)
        ph-main-panel (ui-ph/get-power-hour-wrapper-panel)
        ph-loading-panel (ui-ph-load/get-power-hour-loading-wrapper-panel)
        ph-ctrl-panel (ui-ph-ctrl/get-power-hour-controller-wrapper-panel)
        mf (get-main-frame login-panel)
        roots-to-update [login-panel ph-main-panel ph-loading-panel ph-ctrl-panel]
        potential-token @cmn-session/spotify-session]
    (reset! cmn-ui/app-state
            {:ui-ref mf
             :roots-to-update roots-to-update
             :panels {cmn-ui/ui-scene-login login-panel
                      cmn-ui/ui-scene-power-hour-main ph-main-panel
                      cmn-ui/ui-scene-power-hour-loading ph-loading-panel
                      cmn-ui/ui-scene-power-hour-ctrl ph-ctrl-panel}
             :status nil
             :scene cmn-ui/ui-scene-login
             :authenticated? false
             :user-info nil
             :spotify {:playlists []
                       :selected-playlist-songs nil
                       :selected-playlist-song-count 0
                       :song-data-loaded 0
                       :song-data-to-load 0
                       :ph {:current-song-count 0
                            :total-song-count 0
                            :current-album-art-url nil
                            :current-artist nil
                            :current-album nil}}
             :token nil})
    (state-watcher mf)
    (scene-watcher mf)
    (percent-watcher mf)
    (when potential-token
      (ui-ctrl/handle-successful-login potential-token))
    ;(invoke-later
    ;  (show! mf)
    ;  mf)

    mf

    ))

;(def my-ui (ui))
;
;(invoke-later
;  (show! my-ui))


(def ui-ref-atom (atom nil))

(defn refresh-ui []
  (let [ui-ref (ui)]
    (reset! ui-ref-atom ui-ref)
    (show! ui-ref)))

;;(refresh-ui)
