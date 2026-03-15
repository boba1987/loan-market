(ns loan-market.core
  (:require [loan-market.config :as config]
            [loan-market.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(defn -main [& _]
  (let [port (config/port)]
    (jetty/run-jetty handler/app {:port port :join? true})))
