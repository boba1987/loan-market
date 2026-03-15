(ns loan-market.handler
  (:require [compojure.core :refer [context routes]]
            [loan-market.auth.core :as auth]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.routes.public :as public]
            [loan-market.routes.user :as user-routes]
            [loan-market.routes.bank :as bank-routes]
            [ring.middleware.json :as json]))

(defn build-app
  "Build Ring handler from DB connection."
  [conn]
  (-> (routes
       (public/public-routes conn)
       (context "/api/user" []
         (auth/wrap-jwt (auth/wrap-require-role (user-routes/user-routes) ["user"])))
       (context "/api/bank" []
         (auth/wrap-jwt (auth/wrap-require-role (bank-routes/bank-routes) ["bank"]))))
      json/wrap-json-response
      json/wrap-json-body))

;; When using lein ring server, conn is created at startup via init! (not on first request).
(defonce conn
  (delay (let [c (db/connect)]
           (seed/seed-if-empty! c)
           c)))

(defn init!
  "Force DB connection and seed at server startup. Called by lein ring :init."
  []
  (force conn))

(defn app
  "Ring handler. (app request) for lein ring server; (app conn) returns a handler for lein run."
  [x]
  (if (map? x)
    ;; Ring called (app request) -> return response
    ((build-app @conn) x)
    ;; -main called (app conn) -> return request handler
    (fn [request]
      ((build-app x) request))))
