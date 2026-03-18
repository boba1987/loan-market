(ns loan-market.domain.user
  (:require [datomic.client.api :as d]
            [buddy.hashers :as hashers]))

(defn eid-by-username
  "Return user entity id by username, or nil."
  [conn username]
  (ffirst (d/q '[:find ?e :in $ ?u :where [?e :user/username ?u]]
               (d/db conn)
               (str username))))

(defn find-by-username
  "Return user entity map by username, or nil."
  [conn username]
  (let [db (d/db conn)
        eid (eid-by-username conn username)]
    (when eid
      (d/pull db '[:user/username :user/password-hash :user/role] eid))))

(defn check-password
  "Verify plain password against stored hash. Returns true if match."
  [plain hash]
  (hashers/check plain hash))

(defn hash-password
  [plain]
  (hashers/derive plain))

(defn create!
  "Transact a new user. Password must be plain; it will be hashed. Returns tx result."
  [conn username plain-password role]
  (d/transact conn {:tx-data [{:user/username     (str username)
                              :user/password-hash (hash-password plain-password)
                              :user/role         (str role)}]}))

(defn count-users
  "Return number of users in the database (for empty check)."
  [conn]
  (count (d/q '[:find ?e :where [?e :user/username _]] (d/db conn))))

(defn list-users
  "Return sequence of {:username _ :role _} for all users (for admin/inspection)."
  [conn]
  (let [db-value (d/db conn)
        result (d/q '[:find ?username ?role
                      :where [?e :user/username ?username]
                             [?e :user/role ?role]]
                    db-value)]
    (mapv (fn [[u r]] {:username u :role r}) result)))
