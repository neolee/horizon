(ns net.paradigmx.horizon.meta)

(def codename "horizon")
(def dbname codename)

(defn load-config []
  (if-let [config (read-string (slurp "local/config.edn"))]
    (get-in config [(keyword codename)])
    nil))
