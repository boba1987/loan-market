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
     (fn [_req]
       (-> (response/response {:users (user/list-users conn)})
           (response/content-type "application/json"))))

   (POST "/users" []
     (fn [req]
       (try
         (let [body     (:body req)
               username (or (body-val body :username) (body-val body "username"))
               password (or (body-val body :password) (body-val body "password"))
               role     (or (body-val body :role) (body-val body "role"))
               name     (or (body-val body :name) (body-val body "name"))
               email    (or (body-val body :email) (body-val body "email"))]
           (when (or (or (nil? username) (str/blank? (str username)))
                     (nil? password)
                     (nil? role))
             (throw (ex-info "username, password, role are required"
                             {:field "username/password/role"})))
           (user/create! conn username password role {:name name :email email})
           (-> (response/response {:username username :role (str role)})
               (response/status 201)
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (let [m (.getMessage e)]
             (-> (response/response {:error m})
                 (response/status 400)
                 (response/content-type "application/json"))))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (PUT "/users/:username" [username]
     (fn [req]
       (try
         (let [body    (:body req)
               password (body-val body :password)
               role     (body-val body :role)
               name     (body-val body :name)
               email    (body-val body :email)]
           (when (and (nil? password) (nil? role) (nil? name) (nil? email))
             (throw (ex-info "At least one of password, role, name, email is required"
                             {:username username})))
           (user/update! conn username {:password password :role role :name name :email email})
           (-> (response/response {:username username :updated true})
               (response/status 200)
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 400)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (DELETE "/users/:username" [username]
     (fn [_req]
       (try
         (user/delete! conn username)
         (-> (response/response {:username username :deleted true})
             (response/content-type "application/json"))
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


