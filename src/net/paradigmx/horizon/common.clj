(ns net.paradigmx.horizon.common
  (:require [hashp.core :include-macros true]
            [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]))

;; common macros
;; if-let multiple bindings version
;; bought from https://clojuredocs.org/clojure.core/if-let
;; NOTE `else` branch not work with expressions
(defmacro if-let*
  "Like `core/if-let` but support multiple bindings for `then` iff all tests
  are truthy."
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

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
