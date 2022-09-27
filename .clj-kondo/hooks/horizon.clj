(ns hooks.horizon
  (:require [clj-kondo.hooks-api :as api]))

(defn if-let*
  [{:keys [:node]}]
  (let [[bindings then & [else]] (rest (:children node))
        bindings (api/vector-node (:children bindings))
        tst (list (first bindings))
        else (or else (api/token-node nil))
        if-node (api/list-node
                 (list*
                  (api/token-node 'if)
                  (list tst then else)))
        new-node (api/list-node
                  (list*
                   (api/token-node 'let)
                   bindings
                   (list if-node)))]
    {:node new-node}))
