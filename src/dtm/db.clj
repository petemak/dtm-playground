(ns dtm.db
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]))

(def hhs-schema (read-string (slurp "src/dtm/hhs-schema.edn")))
(def ch6-schema (read-string (slurp "resources/ch6-schema.edn")))
(def ch6-data   (read-string (slurp "resources/ch6-data.edn")))
(def config (read-string (slurp "resources/config.edn") ))



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


;; Datomic is that everything is considered data,
;; including the definition of the schema for your database.
;; Because it is just data, it can be created and manipulated
;; just like any other data you put into the database.
;; To add new schema, you issue a transaction that carries the
;; new schema definition.
(defn transact-test-schema
  "Save schema that defines attributes that can
   can be associate with and entity"
  []
  (d/transact conn ch6-schema))


;; Transact test data
(defn transact-test-data
  []
  (d/transact conn ch6-data))


;;
;; Query all users
(def all-users '[:find ?f ?l
                 :where
                 [?e :user/first-name ?f]
                 [?e :user/last-name ?l]])


(defn find-users
  "Return firs and last name of all users"
  []
  (d/q all-users (d/db conn)))
