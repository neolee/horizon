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

(def common-interceptors [(body-params/body-params) http/html-body])

(defn routes []
  #{["/" :get (conj common-interceptors `home-page)]
    ["/about" :get (conj common-interceptors `about-page)]})


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
