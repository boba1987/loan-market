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
      (d/pull db '[:user/username
                    :user/password-hash
                    :user/role
                    :user/name
                    :user/email] eid))))

(defn check-password
  "Verify plain password against stored hash. Returns true if match."
  [plain hash]
  (hashers/check plain hash))

(defn hash-password
  [plain]
  (hashers/derive plain))

(defn create!
  "Transact a new user. Password must be plain; it will be hashed. Returns tx result."
  ([conn username plain-password role]
   (create! conn username plain-password role {}))
  ([conn username plain-password role {:keys [name email]}]
   (let [tx (cond-> {:user/username     (str username)
                      :user/password-hash (hash-password plain-password)
                      :user/role         (str role)}
              name  (assoc :user/name  (str name))
              email (assoc :user/email (str email)))]
     (d/transact conn {:tx-data [tx]}))))

(defn update!
  "Update a user. Only updates fields present in the map.
   Supported keys: :password (plain), :role, :name, :email.
   Throws ex-info if the user doesn't exist."
  [conn username {:keys [password role name email]}]
  (let [eid (eid-by-username conn username)]
    (when-not eid
      (throw (ex-info "User not found" {:username (str username)})))
    (let [tx-data (cond-> {:db/id eid}
                    password (assoc :user/password-hash (hash-password password))
                    role     (assoc :user/role (str role))
                    name     (assoc :user/name (str name))
                    email    (assoc :user/email (str email)))]
      (d/transact conn {:tx-data [tx-data]}))))

(defn delete!
  "Delete a user by username. Throws ex-info if the user doesn't exist."
  [conn username]
  (let [eid (eid-by-username conn username)]
    (when-not eid
      (throw (ex-info "User not found" {:username (str username)})))
    (let [db (d/db conn)
          u  (d/pull db '[:user/username
                          :user/password-hash
                          :user/role
                          :user/name
                          :user/email] eid)
          tx-data (cond-> [[:db/retract eid :user/username      (:user/username u)]
                            [:db/retract eid :user/password-hash (:user/password-hash u)]
                            [:db/retract eid :user/role          (:user/role u)]]
                    (:user/name u)  (conj [:db/retract eid :user/name  (:user/name u)])
                    (:user/email u) (conj [:db/retract eid :user/email (:user/email u)]) )]
      (d/transact conn {:tx-data [tx-data]}))))

(defn count-users
  "Return number of users in the database (for empty check)."
  [conn]
  (count (d/q '[:find ?e :where [?e :user/username _]] (d/db conn))))

(defn list-users
  "Return sequence of {:username _ :role _} for all users (for admin/inspection)."
  [conn]
  (let [db-value (d/db conn)
        eids (d/q '[:find ?e
                    :where [?e :user/username _]] db-value)]
    (mapv (fn [[eid]]
            (let [u (d/pull db-value '[:user/username :user/role :user/name :user/email] eid)]
              {:username (:user/username u)
               :role     (:user/role u)
               :name     (:user/name u)
               :email    (:user/email u)}))
          eids)))
