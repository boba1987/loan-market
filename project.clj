(defproject com.leiningen-test "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.4"]]
  :profiles {:dev {:dependencies [[midje "1.10.10"]
                                  [criterium "0.5.153-ALPHA"]]
                   :plugins [[lein-midje "3.2.1"]]}})
