(ns loan-market.admin-user-test
  (:require [midje.sweet :refer [facts fact]
                         :refer-macros [=>]]
            [loan-market.config :as config]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.domain.user :as user]))

(facts "admin user listing filters"
  (with-redefs [config/storage-dir (constantly nil)]
    (let [conn (db/connect)]
      (seed/seed-if-empty! conn)

      (fact "GET /api/admin/users?role=admin returns only admins"
        (let [admins (user/list-users conn {:role "admin"})]
          (pos? (count admins)) => true
          (every? #(= (:role %) "admin") admins) => true))

      (fact "role filter works for banks"
        (let [banks (user/list-users conn {:role "bank"})]
          (pos? (count banks)) => true
          (every? #(= (:role %) "bank") banks) => true)))))

