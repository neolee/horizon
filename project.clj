(defproject net.paradigmx.horizon "0.0.1-SNAPSHOT"
  :description "Paradigm X Horizon services"
  :url "http://horizon.paradigmx.net"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.route "0.5.10"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 ;; [io.pedestal/pedestal.immutant "0.5.10"]
                 ;; [io.pedestal/pedestal.tomcat "0.5.10"]

                 [org.clojure/data.json "2.4.0"]

                 ;; track source changes for autoreloading
                 [ns-tracker "0.4.0"]

                 [ch.qos.logback/logback-classic "1.2.11" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.36"]
                 [org.slf4j/jcl-over-slf4j "1.7.36"]
                 [org.slf4j/log4j-over-slf4j "1.7.36"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "net.paradigmx.horizon.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.10"]]}
             :uberjar {:aot [net.paradigmx.horizon.server]}}
  :main ^{:skip-aot true} net.paradigmx.horizon.server)
