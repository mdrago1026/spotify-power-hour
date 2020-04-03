(ns spotify-power-hour.ui.components.power-hour.start
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

(defn get-power-hour-start-panel []
  (let []
    (mig/mig-panel
      :id :ph-start-panel
      :constraints ["fill, flowy"]
      :items [
              [(label :text "Start PH Panel"
                      :class :ph-loading
                      :id :ph-start-label) "cell 0 0 ,align center"]])))

(defn get-power-hour-start-wrapper-panel []
  (mig/mig-panel
    :id :ph-start-panel-wrapper
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(get-power-hour-start-panel) "cell 0 0, align center"]]))
