(ns loan-market.routes.admin
  (:require [compojure.core :refer [GET POST PUT DELETE routes]]
            [clojure.string :as str]
            [loan-market.domain.credit-application :as credit-application]
            [loan-market.domain.data-loader :as data-loader]
            [loan-market.domain.user :as user]
            [ring.util.response :as response]))

(defn- body-val [body k]
  (or (get body k) (get body (name k))))

(defn- parse-int [x]
  (cond
    (integer? x) x
    (number? x) (int x)
    (string? x) (Integer/parseInt (str/trim x))
    :else (throw (ex-info "Invalid integer" {:value x}))))

(defn- parse-double* [x]
  (cond
    (number? x) (double x)
    (string? x) (Double/parseDouble (str/trim x))
    :else (throw (ex-info "Invalid numeric field" {:value x}))))

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
               role     (or (body-val body :role) (body-val body "role"))]
           (when (or (or (nil? username) (str/blank? (str username)))
                     (nil? password)
                     (nil? role))
             (throw (ex-info "username, password, role are required"
                             {:field "username/password/role"})))
           (user/create! conn username password role)
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
               role     (body-val body :role)]
           (when (and (nil? password) (nil? role))
             (throw (ex-info "password or role is required" {:username username})))
           (user/update! conn username {:password password :role role})
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

   ;; Banks (CSV-backed)
   (GET "/banks" []
     (fn [_req]
       (-> (response/response {:banks (data-loader/list-banks)})
           (response/content-type "application/json"))))

   (POST "/banks" []
     (fn [req]
       (try
         (let [body     (:body req)
               id       (parse-int (body-val body :id))
               name     (body-val body :name)
               interest (parse-double* (body-val body :interest))]
           (data-loader/add-bank-to-csv id name interest)
           (-> (response/response {:id id :name name :interest interest})
               (response/status 201)
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 400)
               (response/content-type "application/json")))
         (catch IllegalArgumentException e
           (-> (response/response {:error (.getMessage e)})
               (response/status 409)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (PUT "/banks/:id" [id]
     (fn [req]
       (try
         (let [body     (:body req)
               name     (body-val body :name)
               interest (parse-double* (body-val body :interest))
               bid      (parse-int id)]
           (data-loader/update-bank-in-csv bid name interest)
           (-> (response/response {:id bid :name name :interest interest})
               (response/content-type "application/json")))
         (catch clojure.lang.ExceptionInfo e
           (-> (response/response {:error (.getMessage e)})
               (response/status 404)
               (response/content-type "application/json")))
         (catch IllegalArgumentException e
           (-> (response/response {:error (.getMessage e)})
               (response/status 409)
               (response/content-type "application/json")))
         (catch Exception _
           (-> (response/response {:error "Internal server error"})
               (response/status 500)
               (response/content-type "application/json"))))))

   (DELETE "/banks/:id" [id]
     (fn [_req]
       (try
         (let [bid (parse-int id)]
           (data-loader/delete-bank-from-csv bid)
           (-> (response/response {:id bid :deleted true})
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


