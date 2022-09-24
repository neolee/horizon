(ns horizon.server
  (:gen-class) ; for -main method in uberjar
  (:require [ns-tracker.core :refer [ns-tracker]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [horizon.service :as service]))

(defonce modified-namespaces
  (ns-tracker ["src" "test"]))

(defn watch-routes-fn [routes]
  (fn []
    (doseq [ns-sym (modified-namespaces)]
      (require ns-sym :reload))
    (route/expand-routes routes)))

;; interactive development
(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating :dev server...")
  (-> service/service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes (watch-routes-fn (service/routes))
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;; turn off CSP in dev mode
              ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
      ;; wire up interceptor chains
      http/default-interceptors
      http/dev-interceptors
      http/create-server
      http/start))

(defonce dev-server (atom nil))

(defn start-dev []
  (reset! dev-server (run-dev)))

(defn stop-dev []
  (http/stop dev-server))

(defn test-request [verb url]
  (test/response-for (::http/service-fn @dev-server) verb url))

;; production
(defonce server (http/create-server service/service))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating :prod server...")
  (http/start server))
