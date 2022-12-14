(ns net.paradigmx.horizon.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring]
            [net.paradigmx.horizon.playground :as play]
            [net.paradigmx.horizon.todo :as todo]
            [net.paradigmx.horizon.holiday :as holiday]
            [net.paradigmx.horizon.play24 :as p24]
            [net.paradigmx.horizon.common :as common]))

(defn about-page
  [_request]
  (ring/response (format "Paradigm X Horizon (engine:clojure-%s)"
                         (clojure-version))))

(defn home-page
  [request]
  (let [greeting (if-let [user (get-in request [:params :user])]
                   (str "Hi " user ", ")
                   "Hi, ")]
    (ring/response (str greeting "welcome to the REAL world!"))))

;; the routes
(defn routes []
  #{["/" :get (conj common/html-body `home-page)]
    ["/about" :get (conj common/html-body `about-page)]
    ["/test" :get (conj common/json-body `play/test-json)]
    ["/holiday/:date" :get [http/json-body common/entity-reader
                            common/service-error-handler holiday/holiday-query]]
    ["/calendar/monthly/:date" :get [http/json-body common/entity-reader
                                     common/service-error-handler holiday/calendar-monthly-query]]
    ["/play24/:numbers" :get  [http/json-body common/entity-reader
                                     common/service-error-handler p24/play24-query]]
    ["/todo" :get (conj common/html-body `todo/todos-page)]
    ["/todo" :post [todo/db-interceptor todo/list-create]]
    ["/todo/:list-id" :get [common/coerce-body common/content-negotiator common/entity-reader
                            todo/db-interceptor todo/list-view]]
    ["/todo/:list-id" :post [todo/list-item-view todo/db-interceptor todo/list-item-create]]
    ["/todo/:list-id/:item-id" :get [common/coerce-body common/content-negotiator common/entity-reader
                                     todo/db-interceptor todo/list-item-view]]
    ["/todo/:list-id/:item-id" :put [(body-params/body-params) todo/list-item-view
                                     todo/db-interceptor todo/list-item-update]]
    ["/todo/:list-id/:item-id" :delete [todo/db-interceptor todo/list-item-delete]]
    })

;; the service
(def service
  {:env :prod
   ::http/routes (routes)
   ::http/allowed-origins ["https://frontier.paradigmx.net" "http://localhost:3000" "http://localhost:4173"]
   ::http/resource-path "/public"

   ::http/type :jetty
   ::http/host "127.0.0.1"
   ::http/port 8000
   ::http/container-options {:h2c? true
                             :h2? false
                             :ssl? false}})
