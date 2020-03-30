(ns spotify-client.ui.common)

(def app-state (atom {}))

(def ui-scene-login :login-form-scene)

(def ui-states
  {:login {:sending-request-to-auth-server :sending-request-to-auth-server
           :polling-auth-server-for-token :polling-auth-server-for-token}})
