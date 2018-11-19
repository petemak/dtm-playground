(ns dtm.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]))


(def hhs-schema (read-string (slurp "src/dtm/hhs-schema.edn")))
(def ch6-parts  (read-string (slurp "resources/ch6-partitions.edn")))
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
  '[:find ?e ?f ?l ?mail
    :in $ ?mail
    :where
    [?e :user/email ?mail]
    [?e :user/first-name ?f]
    [?e :user/last-name ?l]])


(defn user-by-mail
  "Find a user with the specified email"
  [email]
  (d/q user-by-mail-query
       (d/db conn)
       email))

;;------------------------------------------------
;; add a new user
;; 
