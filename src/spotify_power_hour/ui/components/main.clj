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
            [spotify-power-hour.ui.components.common :as cmn-ui-comp])
  (:import (javax.swing ImageIcon)
           (java.awt.event KeyEvent)))

(def menu-bar
  (menubar :items
           [
            (menu :text "Menu" :items [])
            ;(menu :text "Jython" :items [jython-item])
            ;(menu :text "Slack" :items [slack-emoji-uploader-menu-item
            ;                            slack-emoji-manage-menu-item])
            ;(menu :text "Config" :items [config-set-file-limit-item])
            ;(menu :text "Debug" :items [debug-show-logs-item debug-set-log-level-item])
            ]))


(defn get-main-frame [content]
  (let [mf (frame :title cfg-ui/app-name
                  :on-close (or (keyword (System/getenv "SEU_ON_CLOSE")) :exit)
                  :content content
                  :menubar menu-bar
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
            (config! (select ui [:#login-success-text]) :visible? true))


          :else
          (do
            ;;  (error (format "Unknown UI state: %s" (new-state :status)))
            (invoke-later
              (info "UI STATE: NIL")
              (config! (select ui [:#login-button]) :enabled? true)
              (config! (select ui [:#login-spinner]) :visible? false)
              (config! (select ui [:.login-form]) :enabled? true)
              (config! (select ui [:#login-success-text]) :visible? false)
              )))))))


;; top-info-pane (cmn-ui-comp/user-info-panel)

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
        mf (get-main-frame login-panel)
        roots-to-update [login-panel]]
    (swap! cmn-ui/app-state assoc
           :ui-ref mf
           :roots-to-update roots-to-update
           :panels {}
           :status nil
           :scene cmn-ui/ui-scene-login)

    ;; TODO: need to loop through and add listeners after all panels are made
    ;(listen (select (cmn/get-panel :halo :login) [:#halo-stats-password]) :key-pressed ui-halo-stats/handle-keys)
    (add-watchers mf)
    ;(add-file-limit-watcher mf)
    ;(scene-watcher mf)
    ;(ui-halo-stats/halo-stats-watcher mf)
    ;(ui-halo-stats/progress-bar-watcher mf)
    ;(ui-jython/jython-watcher mf)
    ;(ui-halo-d4d/halo-d4d-watcher mf)
    ;(ui-slack-manage/slack-manage-emojis-watcher mf)
    (invoke-later
      (show! mf)
      mf)))

;;(ui)
