(ns loan-market.handler
  (:require [compojure.core :refer [context routes]]
            [compojure.route :as route]
            [loan-market.auth.core :as auth]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.routes.admin :refer [admin-routes]]
            [loan-market.routes.public :as public]
            [loan-market.routes.user :as user-routes]
            [loan-market.routes.bank :as bank-routes]
            [ring.middleware.json :as json]))

(defn build-app
  [conn]
  (-> (routes
       (public/public-routes conn)
       (context "/api/user" []
         (auth/wrap-jwt (auth/wrap-require-role (user-routes/user-routes conn) ["user"])))
       (context "/api/bank" []
         (auth/wrap-jwt (auth/wrap-require-role (bank-routes/bank-routes conn) ["bank"])))
       (context "/api/admin" []
         (auth/wrap-jwt (auth/wrap-require-role (admin-routes conn) ["admin"])))
       (route/not-found "Not found"))
      json/wrap-json-response
      json/wrap-json-body))

(defonce conn
  (delay (let [c (db/connect)]
           (seed/seed-if-empty! c)
           c)))

(defn app
  [x]
  (if (map? x)
    ((build-app @conn) x)
    (fn [request]
      ((build-app x) request))))
