(ns spotify-power-hour.ui.components.common
  (:require [seesaw.mig :as mig]
            [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]))

(def user-info-not-logged-in "Not Logged In")

(defn user-info-panel []
  (mig/mig-panel
    :constraints ["fill, flowx"]
    :items [[(label :text user-info-not-logged-in
                    :id :user-info-label
                    :class :top-info-logged-in-text
                    :font (font :name "Lucida Grande" :size 10))
             "cell 0 0"]]))
