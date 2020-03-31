(ns spotify-power-hour.ui.common)

(def app-state (atom {}))

(def ui-scene-login :login-form-scene)

(def ui-states
  {:login {:sending-request-to-auth-server :sending-request-to-auth-server
           :successfully-authed :successfully-authed
           :failed-to-auth :failed-to-auth}})
