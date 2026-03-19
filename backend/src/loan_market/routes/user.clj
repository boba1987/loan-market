(ns loan-market.routes.user
  (:require [compojure.core :refer [GET POST routes]]
            [loan-market.domain.user :as user]
            [loan-market.domain.credit-application :as credit-application]
            [ring.util.response :as response]))

(defn user-routes
  [conn]
  (routes
   (GET "/me" []
     (fn [req]
       (let [u (user/find-by-email conn (:auth/email req))]
         (-> (response/response {:role  (:auth/role req)
                                   :name  (:user/name u)
                                   :email (:user/email u)})
           (response/content-type "application/json"))))

   (POST "/credit-applications" []
     (fn [req]
       (try
         (let [payload  (:body req)
               email     (:auth/email req)
               created   (credit-application/create! conn email payload)]
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
             email (:auth/email req)]
          (-> (response/response (credit-application/list-by-user conn email {:page page :pageSize pageSize}))
            (response/content-type "application/json"))))))))

