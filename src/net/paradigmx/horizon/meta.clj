(ns net.paradigmx.horizon.meta)

(def ^:private meta-name "horizon")

(def db-name meta-name)
(def holiday-table :holiday)

(def config
  (if-let [config (read-string (slurp "local/config.edn"))]
    (get-in config [(keyword meta-name)])
    nil))

(def db-spec (get-in config [:db] {:dbtype "postgresql"
                                   :host "localhost"
                                   :port 5432
                                   :user "paradigmx"
                                   :password "test"
                                   :dbname db-name}))

(def db-type (get-in db-spec [:dbtype] "postgresql"))
(def postgres? (= "postgresql" db-type))
