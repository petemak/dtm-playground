(ns dtm.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]))


(def hhs-schema (read-string (slurp "src/dtm/hhs-schema.edn")))
(def ch6-schema (read-string (slurp "resources/ch6-schema.edn")))
(def ch6-data   (read-string (slurp "resources/ch6-data.edn")))
(def config     (read-string (slurp "resources/config.edn") ))

;; DB URL
(def db-uri "datomic:mem://tutorial")

(defn connet-db
  [] 
  (if (d/create-database db-uri)
    (d/connect db-uri)))


(defn disconnect-db
  [conn]
  (.release conn)
  (d/delete-database db-uri))


;; Must be called in the REPL during interactive development
;; or
;; in the main class when the application launches
(defstate conn :start (connet-db)
               :stop (disconnect-db conn))


;; Oartitions schema
(defn transact-partitions
  []
  (d/transact conn ch6-parts))

;; Datomic is that everything is considered data,
;; including the definition of the schema for your database.
;; Because it is just data, it can be created and manipulated
;; just like any other data you put into the database.
;; To add new schema, you issue a transaction that carries the
;; new schema definition.
(defn transact-user-schema
  "Save schema that defines attributes that can
   can be associate with and entity"
  []
  (d/transact conn ch6-schema))


;; Transact test data
(defn transact-test-data
  []
  (d/transact conn ch6-data))


;;------------------------------------------------
;;
;; Query all users
(def all-users-query
  '[:find ?f ?l
    :where
    [?e :user/first-name ?f]
    [?e :user/last-name ?l]])


(defn all-users
  "Return first and last name of all users"
  []
  (d/q all-users-query
       (d/db conn)))



;;------------------------------------------------
;; Find a user based on login id
;;
(def user-by-lgn-query
  '[:find ?e ?lgn ?f ?l ?m
    :in $ ?lgn
    :where [?e :user/login ?lgn]
           [?e :user/first-name ?f]
           [?e :user/last-name ?l]
           [?e :user/email ?m]])

(defn user-by-lgn
  "Find the user with the specified log-in ID.
  - lgn: the id of the user to find"
  [lgn]
  (-> (d/q user-by-lgn-query
           (d/db conn)
           lgn)
      first))



;;------------------------------------------------
;; Query returns all data of a user
;; who has the given name
(def users-by-name-query
  '[:find ?e ?f ?l ?m
    :in $ ?f
    :where
    [?e :user/first-name ?f]
    [?e :user/last-name ?l]
    [?e :user/email ?m]])


(defn users-by-name
  "Find a user by the given name. Can be first or last name"
  [name]
  (d/q users-by-name-query
       (d/db conn)
       name))


;;------------------------------------------------
;; Find a user bywith the given email
;; 
(def user-by-mail-query
  '[:find ?e ?d ?f ?l ?m
    :in $ ?m
    :where
    [?e :user/email ?m]
    [?e :user/login ?d]
    [?e :user/first-name ?f]
    [?e :user/last-name ?l]])


(defn user-by-mail
  "Find a user with the specified email"
  [email]
  (-> (d/q user-by-mail-query
           (d/db conn)
           email)
      first))



;;------------------------------------------------
;; Return friends of a specific user
;; 
(def friends-query
  '[:find ?fs
    :in $ ?lgn
    :where
    [?e :user/login ?lgn]
    [?e :user/friend ?fs]])



(defn friends
  "Find the entity ids friends of the person specified
   by the given login"
  [lgn]
  (let [fs (d/q friends-query
                (d/db conn)
                lgn)]
    (->> fs
         (map first)
         vec)))

;;------------------------------------------------
;; Query retunrs all users who have a friend
;; with a given name.
(def users-by-friend-query
  '[:find ?e ?f ?l ?m
    :in $ ?name
    :where
    [?e :user/first-name ?f]
    [?e :user/last-name ?l]
    [?e :user/friend ?d]
    [?d :user/first-name ?name]
    [?e :user/email ?m]])

(defn users-by-friend
  "Returns all users who are freinds with a 
   a user with the given first name"
  [friend-name]
  (d/q users-by-friend-query
       (d/db conn)
       friend-name))


;;------------------------------------------------
;; Add a new user
;; 
(defn add-user
  "Transacts a new user to the data storage
   lgn - user login
   f - first name
   l - last name
   e - email
   Example: (db/add-user \"roru\" \"Road\" \"Runner\" \"roru@gmail.com\")"
  [lgn f l e]
  (let [tx-data [{:user/login lgn
                  :user/first-name f
                  :user/last-name l
                  :user/email e}] ]
    (d/transact conn  tx-data)))



;;------------------------------------------------
;; Make 2 people friends of each orther
;; transact a datom [e a v]
;; [e-id1 :user/friend [102 103 108]
(defn make-friends
  "Given 2 user ids, make them friends"
  [lgn1 lgn2]
  (let [friends1 (friends lgn1)
        friends2 (friends lgn2)
        e-id1 (first (user-by-lgn lgn1))
        e-id2 (first (user-by-lgn lgn2))]
    (do
      (let [friends1 (if (nil? friends1) [] friends1)
            friends2 (if (nil? friends2) [] friends2)            
            friends1 (conj friends1 e-id2)
            friends2 (conj friends2 e-id1)]

        (d/transact conn [{:db/id [:user/login lgn1]
                           :user/friend friends1}
                          {:db/id [:user/login lgn2]
                           :user/friend friends2}])))))



(defn results-make-freinds
  "Do :db-after to check results of make-friends transaction
   1) dereffered transaction results containing :db-before, :db-after, 
      and :tx-results
   2) eid - entity identifie e.g. [:user/login \"fred\"]
    "
  [tx-res eid]
  (-> (d/pull (:db-after tx-res) '[:user/friend] eid)
      :user/friend))




