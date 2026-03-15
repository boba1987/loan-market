(ns loan-market.routes.public
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [ring.util.response :as response]))

(defroutes routes
  (GET "/" [] (-> (response/response "hello world")
                  (response/content-type "text/plain")))
  (route/not-found "Not found"))
