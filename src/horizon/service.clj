(ns horizon.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Paradigm X Horizon (engine:clojure-%s)"
                              (clojure-version))))

(defn home-page
  [request]
  (ring-resp/response "Welcome to the REAL world!"))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 200))

;; interceptors
(def common-interceptors [(body-params/body-params) http/html-body])

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok request)]
       (assoc context :response response)))})

;; the routes
(defn routes []
  #{["/" :get (conj common-interceptors `home-page)]
    ["/about" :get (conj common-interceptors `about-page)]
    ["/todo" :post echo :route-name :list-create]
    ["/todo" :get echo :route-name :list-query-form]
    ["/todo/:list-id" :get echo :route-name :list-view]
    ["/todo/:list-id" :post echo :route-name :list-item-create]
    ["/todo/:list-id/:item-id" :get echo :route-name :list-item-view]
    ["/todo/:list-id/:item-id" :put echo :route-name :list-item-update]
    ["/todo/:list-id/:item-id" :delete echo :route-name :list-item-delete]
    })

(def service {:env :prod
              ::http/routes routes

              ::http/resource-path "/public"

              ::http/type :jetty
              ::http/port 8000

              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false
                                        ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                                        }})
