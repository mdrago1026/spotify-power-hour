(defproject spotify-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [clj-time "0.14.4"]
                 [cheshire "5.8.1"]
                 [hikari-cp "1.8.1" ] ;; datbase conn pooling
                 [org.clojure/java.jdbc "0.7.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [http-kit "2.2.0"]
                 [ring-server "0.5.0"]
                 [compojure "1.6.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.1"]]
  :main ^:skip-aot spotify-client.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
