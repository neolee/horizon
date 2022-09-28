(ns net.paradigmx.horizon.service-test
  (:require [clojure.test :as t]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as bootstrap]
            [net.paradigmx.horizon.service :as service]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(t/deftest home-page-test
  (t/is (=
       (:body (pt/response-for service :get "/"))
       "Hi, welcome to the REAL world!"))
  (t/is (=
       (:headers (pt/response-for service :get "/"))
       {"Content-Type" "text/html;charset=UTF-8"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"
        "X-Download-Options" "noopen"
        "X-Permitted-Cross-Domain-Policies" "none"
        "Content-Security-Policy" "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;"})))

(t/deftest about-page-test
  (t/is
   (re-find #"engine:clojure-\d+\.\d+(\.\d+)?"
            (:body (pt/response-for service :get "/about"))))
  
  (t/is (=
       (:headers (pt/response-for service :get "/about"))
       {"Content-Type" "text/html;charset=UTF-8"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"
        "X-Download-Options" "noopen"
        "X-Permitted-Cross-Domain-Policies" "none"
        "Content-Security-Policy" "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;"})))

