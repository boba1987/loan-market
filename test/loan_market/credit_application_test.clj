(ns loan-market.credit-application-test
  (:require [midje.sweet :refer [facts fact =>]]
            [loan-market.config :as config]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.domain.credit-application :as credit-application]))

(facts "credit applications"
  (with-redefs [config/storage-dir (constantly nil)]
    (let [conn (db/connect)]
      (seed/seed-if-empty! conn)

      (fact "create! stores an application for user"
        (let [created (credit-application/create!
                       conn
                       "user"
                       {:name "Jane Doe"
                        :email "jane@example.com"
                        :amount 10000
                        :income 50000
                        :debt 2000
                        :dateOfBirth "1990-01-30"
                        :married true
                        :yearsWorking 7
                        :yearsExperience 5
                        :industry "Software"})]
          (:id created) => some?))

      (fact "list-by-user returns items and supports pagination"
        (let [r1 (credit-application/list-by-user conn "user" {:page 1 :pageSize 1})
              r2 (credit-application/list-by-user conn "user" {:page 2 :pageSize 1})]
          (:page r1) => 1
          (:pageSize r1) => 1
          (:total r1) => pos?
          (count (:items r1)) => 1
        (<= (count (:items r2)) 1) => true))

    (fact "list-all returns all applications (bank use-case)"
      (let [all (credit-application/list-all conn {:page 1 :pageSize 50})]
        (:total all) => pos?
        (<= (count (:items all)) 50) => true)))))

