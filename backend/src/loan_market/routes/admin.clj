(ns loan-market.routes.admin
  (:require [compojure.core :refer [GET POST PUT DELETE routes]]
            [clojure.string :as str]
            [loan-market.domain.credit-application :as credit-application]
            [loan-market.domain.user :as user]
            [ring.util.response :as response]))

(defn- body-val [body k]
  (or (get body k) (get body (name k))))

(defn admin-routes
  "Admin-only routes. Handler is responsible for wrapping JWT + require-role admin."
  [conn]
  (routes
   ;; Users
   (GET "/users" []
     (fn [req]
       (let [role  (some-> (get-in req [:params "role"]) str/trim)
             users (if (or (nil? role) (str/blank? role))
                     (user/list-users conn)
                     (user/list-users conn {:role role}))]
         (-> (response/response {:users users})
             (response/content-type "application/json")))))

   (POST "/users" []
     (fn [req]
       (try
         (let [body     (:body req)
               password (or (body-val body :password) (body-val body "password"))
               role     (or (body-val body :role) (body-val body "role"))
               name     (or (body-val body :name) (body-val body "name"))
               email    (or (body-val body :email) (body-val body "email"))]
           (when (or (or (nil? email) (str/blank? (str email)))
                     (nil? password)
                     (nil? role))
             (throw (ex-info "email, password, role are required"
                             {:field "email/password/role"})))
           (user/create! conn email password role {:name name})
           (let [eid (user/eid-by-email conn email)
                 u   (user/find-by-eid conn eid)]
             (-> (response/response {:id (:db/id u)
                                       :email (:user/email u)
                                       :name (:user/name u)
                                       :role (:user/role u)})
                 (response/status 201)
                 (response/content-type "application/json"))))
         (catch clojure.lang.ExceptionInfo e
           (let [m (.getMessage e)]
             (-> (response/response {:error m})
                 (response/status 400)
                 (response/content-type "application/json"))))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (PUT "/users/:id" [id]
     (fn [req]
       (try
         (let [body    (:body req)
               password (body-val body :password)
               role     (body-val body :role)
               name     (body-val body :name)
               email    (body-val body :email)
               eid      (Long/parseLong (str id))]
           (when (and (nil? password) (nil? role) (nil? name) (nil? email))
             (throw (ex-info "At least one of password, role, name, email is required"
                             {:id id})))
           (user/update-by-eid! conn eid {:password password :role role :name name :email email})
           (let [u (user/find-by-eid conn eid)]
             (-> (response/response {:id (:db/id u)
                                       :email (:user/email u)
                                       :name (:user/name u)
                                       :role (:user/role u)
                                       :updated true})
               (response/status 200)
               (response/content-type "application/json"))))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 400)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (DELETE "/users/:id" [id]
     (fn [_req]
       (try
        (let [eid      (Long/parseLong (str id))
              existing (user/find-by-eid conn eid)]
          (user/delete-by-eid! conn eid)
          (-> (response/response {:id (:db/id existing)
                                    :email (:user/email existing)
                                    :name (:user/name existing)
                                    :role (:user/role existing)
                                    :deleted true})
              (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 404)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   ;; Credit applications
   (GET "/credit-applications" []
     (fn [req]
       (let [page     (some-> (get-in req [:params "page"]) Long/parseLong)
             pageSize (some-> (get-in req [:params "pageSize"]) Long/parseLong)]
         (-> (response/response
              (credit-application/list-all conn {:page page :pageSize pageSize}))
             (response/content-type "application/json")))))

   (DELETE "/credit-applications/:id" [id]
     (fn [_req]
       (try
         (let [cid id]
           (credit-application/delete! conn cid)
           (-> (response/response {:id cid :deleted true})
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 404)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json")))))))

  )


