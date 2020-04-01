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
            [spotify-power-hour.ui.controller :as ui-ctrl]))

(defn get-main-frame [content]
  (let [mf (frame :title cfg-ui/app-name
                  :on-close (or (keyword (System/getenv "SEU_ON_CLOSE")) :exit)
                  :content content
                  :menubar ui-menu/menu-bar
                  :width cfg-ui/ui-width
                  :height cfg-ui/ui-height
                  ;;    :listen [:component-hidden #_hide-frame-cleanup]
                  ;;:resizable? true
                  :id :main-frame)]
    (listen (select mf [:#login-client-id]) :key-pressed ui-login/handle-keys)
    mf))

(defn add-watchers [ui]
  (add-watch
    cmn-ui/app-state :cmn-ui/app-state
    (fn [_ _ old new-state]
      (when (= cmn-ui/ui-scene-login (:scene new-state))
        (cond
          (= (get-in cmn-ui/ui-states [:login :sending-request-to-auth-server]) (new-state :status))
          (invoke-later
            (info "UI STATE: AUTHENTICATING")
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
                     :text cmn-ui-comp/user-info-not-logged-in)
            ;(config! (select ui [:#login-client-id])
            ;         :text "")
            ;(config! (select ui [:#login-redirect-uri])
            ;         :text "")
            )

          :else
          ;;  (error (format "Unknown UI state: %s" (new-state :status)))
          (invoke-later
            (info "UI STATE: NIL")
            (config! (select ui [:#login-button]) :enabled? true)
            (config! (select ui [:#login-spinner]) :visible? false)
            (config! (select ui [:.login-form]) :enabled? true)
            (config! (select ui [:#login-success-text]) :visible? false)
            ))))))

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
                             nil)
                  new-scene (condp = (:scene new-state)
                              cmn-ui/ui-scene-power-hour-main (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-main)
                              cmn-ui/ui-scene-login (cmn-ui/get-panel cmn-ui/ui-scene-login)
                              (do
                                (info "Unknown scene: " (:scene new-state))
                                nil))]
              (when new-scene
                (config! (select ui [:#main-frame]) :content new-scene)
                (config! (select ui [:#main-frame])
                         :title (str cfg-ui/app-name " -- " (name (:scene new-state))))
                (when callback (callback ui))))))))))

(defn get-login-panel []
  (mig/mig-panel
    :id :main-panel
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(ui-login/login-panel) "cell 0 0, align center"]
            ]))

(defn
  ui
  []
  (let [login-panel (get-login-panel)
        ph-main-panel (ui-ph/get-power-hour-wrapper-panel)
        mf (get-main-frame login-panel)
        roots-to-update [login-panel ph-main-panel]]
    (swap! cmn-ui/app-state assoc
           :ui-ref mf
           :roots-to-update roots-to-update
           :panels {cmn-ui/ui-scene-login login-panel
                    cmn-ui/ui-scene-power-hour-main ph-main-panel}
           :status nil
           :scene cmn-ui/ui-scene-login
           :authenticated? false
           :user-info nil
           :spotify {:playlists []}
           :token nil)
    (add-watchers mf)
    (scene-watcher mf)
    (invoke-later
      (show! mf)
      mf)))

;;(def ui-context (ui))


;;(dispose! ui-context)
