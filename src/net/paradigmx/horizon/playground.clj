(ns net.paradigmx.horizon.playground
  (:require [io.pedestal.http :as http]))

(defn test-json [_request]
  (let [data {:name "Neo" :email "neo@soulhacker.me" :available true}]
    (http/json-response data)))
