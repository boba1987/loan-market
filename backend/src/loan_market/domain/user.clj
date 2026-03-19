(ns loan-market.domain.user
  (:require
   [clojure.string :as str]
   [datomic.client.api :as d]
   [buddy.hashers :as hashers]))

(defn eid-by-email
  "Return user entity id by email, or nil."
  [conn email]
  (ffirst (d/q '[:find ?e
                 :in $ ?email
                 :where [?e :user/email ?email]]
               (d/db conn)
               (str email))))

(defn find-by-email
  "Return user entity map by email, or nil."
  [conn email]
  (let [db (d/db conn)
        eid (eid-by-email conn email)]
    (when eid
      (d/pull db '[:user/password-hash
                    :user/role
                    :user/name
                    :user/email] eid))))

(defn find-by-eid
  "Return user entity map by Datomic entity id, or nil."
  [conn eid]
  (let [db (d/db conn)]
    (when eid
      (d/pull db '[:db/id
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

(declare update-by-eid! delete-by-eid!)

(defn create!
  "Transact a new user. Password must be plain; it will be hashed. Returns tx result."
  ([conn email plain-password role]
   (create! conn email plain-password role {}))
  ([conn email plain-password role {:keys [name]}]
   (let [tx (cond-> {:user/email         (str email)
                      :user/password-hash (hash-password plain-password)
                      :user/role          (str role)}
              name (assoc :user/name (str name)))]
     (d/transact conn {:tx-data [tx]}))))

(defn update!
  "Update a user by their current email.
   Supported keys: :password (plain), :role, :name, :email (new email).
   Throws ex-info if the user doesn't exist."
  [conn current-email {:keys [password role name] :as update}]
  (let [eid (eid-by-email conn current-email)]
    (when-not eid
      (throw (ex-info "User not found" {:email (str current-email)})))
    (update-by-eid! conn eid {:password password
                              :role role
                              :name name
                              :email (:email update)})))

(defn update-by-eid!
  "Update a user by Datomic entity id."
  [conn eid {:keys [password role name email]}]
  (let [db (d/db conn)]
    (when-not (d/pull db '[:db/id] eid)
      (throw (ex-info "User not found" {:id eid})))
    (let [tx-data (cond-> {:db/id eid}
                  password (assoc :user/password-hash (hash-password password))
                  role     (assoc :user/role (str role))
                  name     (assoc :user/name (str name))
                  email    (assoc :user/email (str email)))]
      (d/transact conn {:tx-data [tx-data]}))))

(defn delete-by-eid!
  "Delete a user by Datomic entity id."
  [conn eid]
  (let [db (d/db conn)
        u  (d/pull db '[:db/id
                        :user/password-hash
                        :user/role
                        :user/name
                        :user/email] eid)
        _  (when-not u
             (throw (ex-info "User not found" {:id eid})))
        tx-data (cond-> [[:db/retract eid :user/password-hash (:user/password-hash u)]
                          [:db/retract eid :user/role          (:user/role u)]]
                  (:user/name u)  (conj [:db/retract eid :user/name  (:user/name u)])
                  (:user/email u) (conj [:db/retract eid :user/email (:user/email u)]) )]
    (d/transact conn {:tx-data [tx-data]})))

(defn delete!
  "Delete a user by email. Throws ex-info if the user doesn't exist."
  [conn email]
  (let [eid (eid-by-email conn email)]
    (when-not eid
      (throw (ex-info "User not found" {:email (str email)})))
    (delete-by-eid! conn eid)))

(defn count-users
  "Return number of users in the database (for empty check)."
  [conn]
  (count (d/q '[:find ?e :where [?e :user/email _]] (d/db conn))))

(defn list-users
  "Return sequence of {:id <eid> :email <...> :name <...> :role <...>} for all users.
   Optionally filters by role via {:role \"admin\"}."
  ([conn] (list-users conn {}))
  ([conn {:keys [role]}]
   (let [db-value (d/db conn)
         eids (if (or (nil? role) (str/blank? (str role)))
                (d/q '[:find ?e
                       :where [?e :user/email _]] db-value)
                (d/q '[:find ?e
                       :in $ ?role
                       :where [?e :user/role ?role]]
                     db-value
                     (str role)))]
     (mapv (fn [[eid]]
             (let [u (d/pull db-value '[:db/id :user/role :user/name :user/email] eid)]
               {:id    (:db/id u)
                :email (:user/email u)
                :name  (:user/name u)
                :role  (:user/role u)}))
           eids))))
