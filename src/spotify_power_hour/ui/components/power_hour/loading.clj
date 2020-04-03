(ns spotify-power-hour.ui.components.power-hour.loading
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

(defn get-power-hour-loading-panel []
  (mig/mig-panel
    :id :ph-loading-panel
    :constraints ["fill, flowy"]
    :items [
            [(label :text "Preparing your playlist..."
                    :class :ph-loading
                    :id :ph-loading-label) "cell 0 0, align center"]
            [(progress-bar
               :id :ph-loading-progress-bar
               :orientation :horizontal
               :paint-string? true
               :value 0)
             "cell 0 0, align center"]
            ]))

(defn get-power-hour-loading-wrapper-panel []
  (mig/mig-panel
    :id :ph-loading-panel-wrapper
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(get-power-hour-loading-panel) "cell 0 0, align center"]]))
