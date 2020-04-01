(ns spotify-power-hour.ui.components.power-hour.main
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [clojure.java.io :as io]
            [spotify-power-hour.ui.components.common :as cmn-ui-comp]))

(defn get-power-hour-main-panel []
  (mig/mig-panel
    :id :login-panel
    :constraints ["fill, flowy"]
    :items [
            [(cmn-ui-comp/user-info-panel) "cell 0 0, aligny top, growx"]
            [(label "Power Hour Main Panel") "cell 0 0, grow"]
            ]))
