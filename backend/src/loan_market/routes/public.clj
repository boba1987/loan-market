(ns loan-market.routes.public
  (:require [compojure.core :refer [GET POST routes]]
            [loan-market.auth.core :as auth]
            [ring.util.response :as response]))

(defn public-routes [conn]
  (routes
   (GET "/" [] (-> (response/response "hello world")
                   (response/content-type "text/plain")))
   (POST "/api/login" [] (auth/login-handler conn))
   ))
