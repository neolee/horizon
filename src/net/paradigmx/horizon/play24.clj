(ns net.paradigmx.horizon.play24
  (:gen-class)
  (:require [clojure.math.combinatorics :as c],
            [clojure.walk :as w]
            [net.paradigmx.common.core :refer [parse-int-arg]]))

(def ^:private op-maps
  (map #(zipmap [:o1 :o2 :o3] %) (c/selections '(* + - /) 3)))

(def ^:private patterns '((:o1 (:o2 :n1 :n2) (:o3 :n3 :n4))
                          (:o1 :n1 (:o2 :n2 (:o3 :n3 :n4)))
                          (:o1 (:o2 (:o3 :n1 :n2) :n3) :n4)))

(defn- eval-safe [exp]
  (try
    (= (eval exp) 24)
    (catch ArithmeticException _e false)))

(defn solutions
  "return a lazy seq which contains all solutions for given numbers,
  each solution is an s-exp like `(* (+ 11 7) (/ 12 9))`"
  [numbers]
  {:pre [(= (count numbers) 4)
         (every? #(not= 0 %) numbers)]}
  (->> (for [om op-maps dm (->> numbers sort c/permutations
                                (map #(zipmap [:n1 :n2 :n3 :n4] %)))]
         (w/prewalk-replace dm (w/prewalk-replace om patterns)))
       (apply concat)
       (filter eval-safe)))

(defn -main [& args]
  (->> (solutions (map parse-int-arg args))
       (map println)
       (doall)
       (count)
       (str "total: ")
       (println)))
