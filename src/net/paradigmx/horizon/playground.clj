(ns net.paradigmx.horizon.playground
    (:require [hashp.core :include-macros true]
              [io.pedestal.http :as http]
              [net.paradigmx.horizon.common :as common]))

(defn test-json [_request]
  (let [data {:name "Neo" :email "neo@soulhacker.me" :available true}]
    (http/json-response data)))
