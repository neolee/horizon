(ns net.paradigmx.horizon.meta)

(def ^:private meta-name "horizon")

(def db-name meta-name)
(def holiday-table :holiday)

(defn load-config []
  (if-let [config (read-string (slurp "local/config.edn"))]
    (get-in config [(keyword meta-name)])
    nil))
