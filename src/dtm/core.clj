(ns dtm.core
  (:require [org.httpkit.server :as s]
            [compojure.core :refer [routes POST GET ANY]])
           
  (:gen-class))

(defonce ^:private server (atom {}))





(defn handler
  [request]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body "<h1>Hello there! </h1>"})


(defn app
  []
  (routes
   (GET "/" []  (str "<h1>Hello!</h1>"))
   (GET "/user/:id" [id] (str "<h1>hello " id "</h1">))))


(defn create-server
  []
  (s/run-server (app), {:port 9080}))

(defn stop-server
  [server]
  (server :timeout 100))


(defn run-server
  []
  (stop-server server)
  (reset! server (create-server)))


