(ns spotify-power-hour.ui.components.login
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
            [spotify-power-hour.spotify.api :as api-spotify]
            [clojure.java.browse :as browse]
            [spotify-power-hour.ui.common :as cmn-ui]
            [spotify-power-hour.controller.spotify :as ctrl-spotify]
            [spotify-power-hour.common.session :as cmn-session]
            [spotify-power-hour.ui.components.common :as cmn-comp]
            [spotify-power-hour.ui.controller :as ui-ctrl])
  (:import (java.awt.event KeyEvent)
           (javax.swing ImageIcon)))

(defn
  handle-keys
  [e]
  (condp = (.getKeyCode e)
    KeyEvent/VK_ENTER (do
                        (ui-ctrl/handle-client-id-submit e)
                        nil)
    ;(when-not (= (get-in config/ui-states [:auth :authenticating])
    ;             (:status @cmn/app-state))
    ;  (info "Pressed Enter")
    ;  ;;(login-handler e))
    ;  nil)
    nil
    ))

(def login-form
  (mig/mig-panel
    :constraints ["fill"]
    :items [[(label "Client ID") "cell 0 0, grow"]
            [(text :editable? true
                   :id :login-client-id
                   :class :login-form
                   :text (or (System/getenv "SPH_DEFAULT_CLIENT_ID") "")
                   :columns 20) "cell 0 0"]
            [(label "Redirect URI") "cell 0 1, grow"]
            [(text :editable? true
                   :id :login-redirect-uri
                   :class :login-form
                   :text (or (System/getenv "SPH_DEFAULT_REDIRECT_URI") "")
                   :columns 20) "cell 0 1"]
            [(button :text "Login"
                     :class :login-form
                     :listen [:action ui-ctrl/handle-client-id-submit]
                     :id :login-button)
             "cell 0 2, align center"]
            [(label :icon (ImageIcon. (io/resource "ajax-loader.gif"))
                    :id :login-spinner :visible? false)
             "cell 0 3, align center"]
            [(label :text "Failed to login. Please try again."
                    :foreground (color "#ff0000")
                    :visible? false
                    :id :login-failed-text) "cell 0 4, align center"]
            [(label :text "Successfully logged in!"
                    :foreground (color "#03fc0f")
                    :visible? false
                    :id :login-success-text) "cell 0 5, align center"]
            ]))

(defn login-panel []
  (mig/mig-panel
    :id :login-panel
    :constraints ["fill, flowy"]
    :items [
            [login-form "cell 0 0, align center"]
            ]))
