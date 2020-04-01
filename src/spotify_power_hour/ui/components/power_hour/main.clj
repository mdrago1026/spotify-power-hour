(ns spotify-power-hour.ui.components.power-hour.main
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
            [spotify-power-hour.ui.common :as ui-cmn])
  (:import (javax.swing ImageIcon)))

(get-in @ui-cmn/app-state [:spotify :playlists])

(defn get-power-hour-main-panel []
  (let [cb (combobox
             :model []
             :renderer (fn [this {:keys [value]}]
                         (text! this
                                (:name value)))
             :listen [:selection (fn [e]
                                   (info "Chose: " (text e)))]
             :id :ph-main-select)]
    (mig/mig-panel
      :id :ph-main-panel
      :constraints ["fill, flowy"]
      :items [
              [(label :text "Select a Playlist"
                      :id :ph-main-select-label) "align center"]
              [cb "cell 0 0, align center"]
              [(label :icon (ImageIcon. (io/resource "ajax-loader.gif"))
                      :id :ph-main-select-spinner :visible? false)
               "cell 0 1, align center"]
              ])))

(defn get-power-hour-wrapper-panel []
  (mig/mig-panel
    :id :ph-main-panel-wrapper
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(get-power-hour-main-panel) "cell 0 0, align center"]
            ]))
