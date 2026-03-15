(ns loan-market.handler
  (:require [loan-market.routes.public :as public]))

(def app
  "Ring handler; used by loan-market.core to start the server."
  public/routes)
