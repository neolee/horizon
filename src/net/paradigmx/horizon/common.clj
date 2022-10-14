(ns net.paradigmx.horizon.common
  (:require [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.error :as error]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]))

;; http helpers
(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def no-content (partial response 204))

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(defn accepted-type [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

;; common interceptors
(def html-body [(body-params/body-params) http/html-body])

(def json-body [(body-params/body-params) http/json-body])

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok request)]
       (assoc context :response response)))})

(def content-negotiator (content-negotiation/negotiate-content supported-types))

(def coerce-body
  {:name :coerce-body
   :leave
   (fn [context]
     (cond-> context
       (nil? (get-in context [:response :headers "Content-Type"]))
       (update-in [:response] coerce-to (accepted-type context))))})

(def entity-reader
  {:name :entity-reader
   :leave
   (fn [context]
     (if-let [object (:result context)]
       (assoc context :response (ok object))
       context))})

(def service-error-handler
  (error/error-dispatch [ctx ex]
                        [{:exception-type :java.time.format.DateTimeParseException}]
                        (assoc ctx :response {:status 400 :body "Bad date format"})
                        [{:exception-type :java.lang.NumberFormatException}]
                        (assoc ctx :response {:status 400 :body "Bad number format"})
                        [{:exception-type :java.lang.AssertionError}]
                        (assoc ctx :response {:status 400 :body "Bad input"})
                        :else
                        (assoc ctx :io.pedestal.interceptor.chain/error ex)))
