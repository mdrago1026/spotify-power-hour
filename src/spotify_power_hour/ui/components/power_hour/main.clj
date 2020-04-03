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
            [spotify-power-hour.ui.controller :as ui-ctrl]
            [spotify-power-hour.ui.common :as ui-cmn]
            [spotify-power-hour.ui.common :as cmn-ui])
  (:import (javax.swing ImageIcon)))

(defn get-power-hour-main-panel []
  (let [song-count-cb (combobox
                        :model ui-cmn/ui-ph-song-count-defaults
                        :enabled? false
                        :renderer (fn [this {:keys [value]}]
                                    (text! this value))
                        :listen [:selection (fn [e]
                                              (let [selection (selection (select (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-main)
                                                                                 [:#ph-main-select-song-count]))]
                                                (info "Song count selection: "selection)
                                                (ui-ctrl/ph-handle-song-count-select e selection)))]
                        :id :ph-main-select-song-count)
        playlist-cb (combobox
                      :model []
                      :renderer (fn [this {:keys [value]}]
                                  (text! this
                                         (:name value)))
                      :listen [:selection (fn [e]
                                            (let [selection (selection (select (cmn-ui/get-panel cmn-ui/ui-scene-power-hour-main)
                                                                               [:#ph-main-select-playlist]))]
                                              (ui-ctrl/ph-select-playlist e selection)))]
                      :id :ph-main-select-playlist)]
    (mig/mig-panel
      :id :ph-main-panel
      :constraints ["fill, flowy"]
      :items [
              [(label :text "Select a Playlist"
                      :class :ph-main
                      :id :ph-main-select-label) "cell 0 0 ,align center"]
              [playlist-cb "cell 0 0, align center"]
              [(label :text cmn-ui-comp/ph-default-playlist-count-text
                      :class :ph-main
                      :id :ph-main-selected-playlist-song-count-label) "cell 0 1 ,align center"]
              [(label :text "Power Hour Song Count"
                      :class :ph-main
                      :id :ph-main-select-label) "cell 0 2, align center"]
              [song-count-cb "cell 0 2, align center"]
              [(label :icon (ImageIcon. (io/resource "ajax-loader.gif"))
                      :class :ph-main
                      :id :ph-main-select-spinner :visible? false)
               "cell 0 3, align center"]
              [(label :text "Selected playlists does not have minimum number of songs"
                      :class :ph-main
                      :foreground (color "#ff0000")
                      :visible? false
                      :id :ph-main-not-enough-songs-error) "cell 0 4, align center"]
              [(button :text "Start Power Hour!"
                       :visible? false
                       :class :ph-main
                       :listen [:action (fn [e]
                                          (info "CLICKED START PH!!!"))]
                       :id :ph-main-start-ph-btn)
               "cell 0 5, align center"]
              ])))

(defn get-power-hour-wrapper-panel []
  (mig/mig-panel
    :id :ph-main-panel-wrapper
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(get-power-hour-main-panel) "cell 0 0, align center"]
            ]))
