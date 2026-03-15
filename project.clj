(defproject com.leiningen-test "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [ring/ring-core "1.12.0"]
                 [ring/ring-jetty-adapter "1.12.0"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.1"]
                 [org.slf4j/slf4j-simple "2.0.9"]
                 [com.datomic/local "1.0.291"]
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-sign "3.4.1"]]
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler loan-market.handler/app
         :init    loan-market.handler/init!
         :port 3000
         :auto-reload? true
         :auto-refresh? true}
  :main loan-market.core
  :profiles {:dev {:dependencies [[midje "1.10.10"]
                                  [criterium "0.5.153-ALPHA"]]
                   :plugins [[lein-midje "3.2.1"]]}})
