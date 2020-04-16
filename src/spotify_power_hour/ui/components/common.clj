(ns spotify-power-hour.ui.components.common
  (:require [seesaw.mig :as mig]
            [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [spotify-power-hour.ui.common :as cmn-ui]))

(def user-info-not-logged-in "Not Logged In")
(def ph-default-playlist-count-text "Selected Playlist Song Count: ")
(def ph-default-playlist-over-60-count-text "Songs at least 60 sec: ")
(def ph-selected-playlist-not-enough-songs "Selected playlist must have at least %s songs")

(defn user-info-panel []
  (mig/mig-panel
    :constraints ["fill, flowx"]
    :items [[(label :text user-info-not-logged-in
                    :id :user-info-label
                    :class :top-info-logged-in-text
                    :font (font :name "Lucida Grande" :size 10))
             "cell 0 0"]]))

(defn update-shared-user-info-component [text]
  (doseq [root (@cmn-ui/app-state :roots-to-update)]
    (config! (select root [:.top-info-logged-in-text])
             :text text)))
