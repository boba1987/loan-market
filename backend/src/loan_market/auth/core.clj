(ns loan-market.auth.core
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as str]
            [loan-market.config :as config]
            [loan-market.domain.user :as user]
            [ring.util.response :as response]))

(def ^:private jwt-secret (memoize config/jwt-secret))
(def default-exp-secs (* 24 60 60)) ;; 24 hours

(defn sign-token
  [username role]
  (let [now (quot (System/currentTimeMillis) 1000)
        claims {:username username
                :role     role
                :exp      (+ now default-exp-secs)
                :iat      now}]
    (jwt/sign claims (jwt-secret))))

(defn unsign-token
  [token]
  (try
    (jwt/unsign token (jwt-secret))
    (catch Exception _ nil)))

(defn login-handler
  "Ring handler: POST body {:username \"\" :password \"\"}. Returns 200 {:token \"\" :role \"\" :name \"\" :email \"\"} or 401."
  [conn]
  (fn [request]
    (let [body    (get request :body)
          username (or (get body :username) (get body "username"))
          password (or (get body :password) (get body "password"))]
      (if (or (str/blank? (str username)) (str/blank? (str password)))
        (-> (response/response {:error "username and password required"})
            (response/status 400)
            (response/content-type "application/json"))
        (if-let [u (user/find-by-username conn username)]
          (if (user/check-password password (:user/password-hash u))
            (-> (response/response {:token (sign-token username (:user/role u))
                                   :role  (:user/role u)
                                   :name  (:user/name u)
                                   :email (:user/email u)})
                (response/status 200)
                (response/content-type "application/json"))
            (-> (response/response {:error "Invalid credentials"})
                (response/status 401)
                (response/content-type "application/json")))
          (-> (response/response {:error "Invalid credentials"})
              (response/status 401)
              (response/content-type "application/json")))))))

(defn wrap-jwt
  "Middleware: extract Authorization Bearer token, unsign, assoc :auth/username and :auth/role to request. 401 if missing/invalid."
  [handler]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"])
          token       (when (and (string? auth-header)
                                 (str/starts-with? (str/lower-case auth-header) "bearer "))
                        (str/trim (subs auth-header 7)))
          claims      (when token (unsign-token token))]
      (if (and claims (:username claims) (:role claims))
        (handler (assoc request :auth/username (:username claims) :auth/role (:role claims)))
        (-> (response/response {:error "Missing or invalid authorization"})
            (response/status 401)
            (response/content-type "application/json"))))))

(defn wrap-require-role
  "Middleware: require request :auth/role to be one of allowed-roles. 403 otherwise."
  [handler allowed-roles]
  (let [allowed (set (map str allowed-roles))]
    (fn [request]
      (if (allowed (get request :auth/role))
        (handler request)
        (-> (response/response {:error "Forbidden"})
            (response/status 403)
            (response/content-type "application/json"))))))
