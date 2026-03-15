(ns loan-market.routes.user
  (:require [compojure.core :refer [GET routes]]
            [ring.util.response :as response]))

(defn user-routes
  "Routes under /api/user (wrap with wrap-jwt and wrap-require-role [\"user\"] in handler)."
  []
  (routes
   (GET "/me" [] (fn [req]
                   (-> (response/response {:username (:auth/username req)
                                           :role     (:auth/role req)})
                       (response/content-type "application/json"))))))
