(ns loan-market.routes.bank
  (:require [compojure.core :refer [GET routes]]
            [loan-market.domain.credit-application :as credit-application]
            [ring.util.response :as response]))

(defn bank-routes
  [conn]
  (routes
   (GET "/me" []
     (fn [req]
       (-> (response/response {:username (:auth/username req)
                               :role     (:auth/role req)})
           (response/content-type "application/json"))))

   (GET "/credit-applications" []
     (fn [req]
       (let [page     (some-> (get-in req [:params "page"]) Long/parseLong)
             pageSize (some-> (get-in req [:params "pageSize"]) Long/parseLong)]
         (-> (response/response (credit-application/list-all conn {:page page :pageSize pageSize}))
             (response/content-type "application/json")))))))
