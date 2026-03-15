(defproject com.leiningen-test "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [ring/ring-core "1.12.0"]
                 [ring/ring-jetty-adapter "1.12.0"]
                 [compojure "1.7.1"]
                 [org.slf4j/slf4j-simple "2.0.9"]]
  :main loan-market.core
  :profiles {:dev {:dependencies [[midje "1.10.10"]
                                  [criterium "0.5.153-ALPHA"]]
                   :plugins [[lein-midje "3.2.1"]]}})
