(defproject holiday "0.1.0-SNAPSHOT"
  :description "Utility to pull and process holiday data"
  :url "https://soulhacker.me"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-http "3.12.3"]
                 [tick "0.5.0"]
                 [org.mariadb.jdbc/mariadb-java-client "3.0.8"]
                 [mysql/mysql-connector-java "8.0.30"]
                 [com.github.seancorfield/honeysql "2.3.928"]
                 [com.github.seancorfield/next.jdbc "1.3.834"]]
  :repl-options {:init-ns net.paradigmx.util.holiday}
  :main ^{:skip-aot true} net.paradigmx.util.holiday)
