(ns loan-market.core
  (:require [loan-market.config :as config]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(defn -main [& _]
  (config/jwt-secret) ; fail fast if JWT_SECRET missing
  (let [conn (db/connect)]
    (seed/seed-if-empty! conn)
    (jetty/run-jetty (handler/app conn) {:port (config/port) :join? true})))
