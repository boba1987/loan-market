(ns loan-market.routes.user
  (:require [compojure.core :refer [GET POST routes]]
            [loan-market.domain.credit-application :as credit-application]
            [ring.util.response :as response]))

(defn user-routes
  "Routes under /api/user (wrap with wrap-jwt and wrap-require-role [\"user\"] in handler)."
  [conn]
  (routes
   (GET "/me" []
     (fn [req]
       (-> (response/response {:username (:auth/username req)
                               :role     (:auth/role req)})
           (response/content-type "application/json"))))

   (POST "/credit-applications" []
     (fn [req]
       (try
         (let [payload  (:body req)
               username (:auth/username req)
               created  (credit-application/create! conn username payload)]
           (-> (response/response created)
               (response/status 201)
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (let [{:keys [field dateOfBirth]} (ex-data e)]
             (-> (response/response {:error (.getMessage e)
                                     :field field
                                     :dateOfBirth dateOfBirth})
                 (response/status 400)
                 (response/content-type "application/json"))))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (GET "/credit-applications" []
     (fn [req]
       (let [page     (some-> (get-in req [:params "page"]) Long/parseLong)
             pageSize (some-> (get-in req [:params "pageSize"]) Long/parseLong)
             username (:auth/username req)]
         (-> (response/response (credit-application/list-by-user conn username {:page page :pageSize pageSize}))
             (response/content-type "application/json")))))))
