(defproject spotify-power-hour "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [clj-time "0.14.4"]
                 [cheshire "5.8.1"]
                ;; [hikari-cp "1.8.1" ] ;; datbase conn pooling
                ;; [org.clojure/java.jdbc "0.7.3"]
               ;;  [org.postgresql/postgresql "42.1.4"]
               ;;  [http-kit "2.2.0"]
               ;;  [ring-server "0.5.0"]
               ;;  [compojure "1.6.0"]
               ;  [ring/ring-json "0.4.0"]
               ;  [ring/ring-defaults "0.3.1"]
               ;  [ring/ring-codec "1.1.2"]
                 [seesaw "1.5.0"]
                 [listora/again "1.0.0"]
                 [ring/ring-codec "1.1.2"]]
  :main ^:skip-aot spotify-power-hour.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
