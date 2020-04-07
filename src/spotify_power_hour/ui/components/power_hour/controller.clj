(ns spotify-power-hour.ui.components.power-hour.controller
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [clojure.java.io :as io]
            [spotify-power-hour.ui.components.common :as cmn-ui-comp]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal]]
            [spotify-power-hour.ui.controller :as ui-ctrl]
            [spotify-power-hour.ui.common :as ui-cmn]
            [spotify-power-hour.ui.common :as cmn-ui])
  (:import (javax.swing ImageIcon)))

(defn get-power-hour-controller-panel []
  (mig/mig-panel
    :id :ph-controller-panel
    :constraints ["fill, flowy, debug"]
    :items [[(button :text "<-"
                     :class :login-form
                    ;; :listen [:action ui-ctrl/handle-client-id-submit]
                     :id :login-button)
             "cell 0 2"]
            [(button :text "->"
                     :class :login-form
                     ;; :listen [:action ui-ctrl/handle-client-id-submit]
                     :id :login-button)
             "cell 3 2"]

            [(label :icon "https://i.scdn.co/image/ab67616d00001e021522bd2a4ea3d69e17f19429")
             "cell 1 1 2 3, align center"] ;; take up 2x3 = 6 grid boxes
            [(label :text "Song Name"
                    :class :ph-loading
                    :id :ph-ctrl-song-name) "cell 1 4, align center"]
            [(label :text "Artist Name"
                    :class :ph-ctrl-artist-name
                    :id :ph-ctrl-song-name) "cell 1 5, align center"]
            ]))

(defn get-power-hour-controller-wrapper-panel []
  (mig/mig-panel
    :id :ph-controller-panel-wrapper
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(get-power-hour-controller-panel) "cell 0 0, align center"]]))
