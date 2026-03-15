(ns loan-market.routes.public
  (:require [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [loan-market.auth.core :as auth]
            [loan-market.domain.user :as user]
            [ring.util.response :as response]))

(defn public-routes [conn]
  (routes
   (GET "/" [] (-> (response/response "hello world")
                   (response/content-type "text/plain")))
   (POST "/api/login" [] (auth/login-handler conn))
   (GET "/api/admin/users" []
        (fn [_] (response/response {:users (user/list-users conn)})))
   (route/not-found "Not found")))
