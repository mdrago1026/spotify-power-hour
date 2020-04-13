(ns spotify-power-hour.ui.components.menubar
  (:require [seesaw.mig :as mig]
            [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.mig :as mig]
            [seesaw.color :refer :all]
            [seesaw.chooser :refer :all]
            [seesaw.font :refer :all]
            [spotify-power-hour.ui.controller :as ui-ctrl]))

(def account-logout-menu-item
  (menu-item :text "Logout"
             :listen [:action ui-ctrl/menu-account-logout-handler]))

(def debug-playlist-selection-menu-item
  (menu-item :text "Playlist Selection"
             :listen [:action ui-ctrl/menu-debug-playlist-selection-handler]))


(def menu-bar
  (menubar :items
           [
            (menu :text "Account" :items [account-logout-menu-item])
            (menu :text "Debug" :items [debug-playlist-selection-menu-item])
            ]))

