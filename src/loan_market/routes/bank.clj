(ns loan-market.routes.bank
  (:require [compojure.core :refer [GET routes]]
            [ring.util.response :as response]))

(defn bank-routes
  "Routes under /api/bank (wrap with wrap-jwt and wrap-require-role [\"bank\"] in handler)."
  []
  (routes
   (GET "/me" [] (fn [req]
                   (-> (response/response {:username (:auth/username req)
                                           :role     (:auth/role req)})
                       (response/content-type "application/json"))))))
