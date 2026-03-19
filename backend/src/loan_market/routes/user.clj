(ns loan-market.routes.user
  (:require [compojure.core :refer [GET POST PUT routes]]
            [loan-market.domain.user :as user]
            [loan-market.domain.credit-application :as credit-application]
            [ring.util.response :as response]))

(defn- body-val [body k]
  (let [missing ::missing
        v1 (get body k missing)
        v2 (get body (name k) missing)]
    (cond
      (not= v1 missing) v1
      (not= v2 missing) v2
      :else nil)))

(defn user-routes
  [conn]
  (routes
   (GET "/me" []
     (fn [req]
       (let [u (user/find-by-email conn (:auth/email req))]
         (-> (response/response {:role         (:auth/role req)
                                   :name         (:user/name u)
                                   :email        (:user/email u)
                                   :dateOfBirth  (:user/date-of-birth u)
                                  :maritalStatus (or (:user/marital-status u)
                                                     (when (contains? u :user/married)
                                                       (if (:user/married u) "married" "not married")))
                                   :yearsWorking (:user/years-working u)
                                   :industry     (:user/industry u)})
             (response/content-type "application/json")))))

   (PUT "/me" []
     (fn [req]
       (try
         (let [body (:body req)
               email (:auth/email req)
               name (body-val body :name)
               dateOfBirth (body-val body :dateOfBirth)
               maritalStatus (body-val body :maritalStatus)
               yearsWorking (body-val body :yearsWorking)
               industry (body-val body :industry)]
          (when (and (nil? name) (nil? dateOfBirth) (nil? maritalStatus) (nil? yearsWorking) (nil? industry))
            (throw (ex-info "At least one of name, dateOfBirth, maritalStatus, yearsWorking, industry is required" {})))
           (user/update! conn email {:name name
                                     :dateOfBirth dateOfBirth
                                    :maritalStatus maritalStatus
                                     :yearsWorking yearsWorking
                                     :industry industry})
           (let [u (user/find-by-email conn email)]
             (-> (response/response {:role         (:auth/role req)
                                     :name         (:user/name u)
                                     :email        (:user/email u)
                                     :dateOfBirth  (:user/date-of-birth u)
                                    :maritalStatus (or (:user/marital-status u)
                                                       (when (contains? u :user/married)
                                                         (if (:user/married u) "married" "not married")))
                                     :yearsWorking (:user/years-working u)
                                     :industry     (:user/industry u)
                                     :updated      true})
                 (response/content-type "application/json"))))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 400)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (POST "/credit-applications" []
     (fn [req]
       (try
         (let [payload (:body req)
               email   (:auth/email req)
               created (credit-application/create! conn email payload)]
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
             email    (:auth/email req)]
         (-> (response/response
               (credit-application/list-by-user conn email {:page page :pageSize pageSize}))
             (response/content-type "application/json")))))))

