(ns loan-market.routes.bank
  (:require [compojure.core :refer [GET POST routes]]
            [loan-market.domain.user :as user]
            [loan-market.domain.credit-application :as credit-application]
            [ring.util.response :as response]))

(defn- body-val [body k]
  (or (get body k) (get body (name k))))

(defn bank-routes
  [conn]
  (routes
   (GET "/me" []
     (fn [req]
       (let [u (user/find-by-username conn (:auth/username req))]
         (-> (response/response {:username (:auth/username req)
                                   :role     (:auth/role req)
                                   :name     (:user/name u)
                                   :email    (:user/email u)})
             (response/content-type "application/json")))))

   (GET "/credit-applications" []
     (fn [req]
       (let [page     (some-> (get-in req [:params "page"]) Long/parseLong)
             pageSize (some-> (get-in req [:params "pageSize"]) Long/parseLong)]
         (-> (response/response (credit-application/list-by-bank
                                   conn
                                   (:auth/username req)
                                   {:page page :pageSize pageSize}))
             (response/content-type "application/json")))))

   (POST "/credit-applications/:id/offer" [id]
     (fn [req]
       (let [body          (:body req)
             bank-username (:auth/username req)
             ir            (body-val body :interestRate)
             rp            (body-val body :repaymentPeriod)]
         (try
           (credit-application/offer! conn bank-username id {:interestRate ir
                                                                 :repaymentPeriod rp})
           (-> (response/response {:id id :offered true})
               (response/content-type "application/json"))
           (catch clojure.lang.ExceptionInfo e
             (let [{:keys [field]} (ex-data e)]
               (-> (response/response {:error (.getMessage e)
                                        :field field})
                   (response/status 400)
                   (response/content-type "application/json"))))
           (catch Exception _
             (-> (response/response {:error "Internal server error"})
                 (response/status 500)
                 (response/content-type "application/json")))))))))
