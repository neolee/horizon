(defproject play24 "1.0.0"
  :description "The game of calculating 24"
  :url "https://soulhacker.me"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]
                 [org.clojure/math.combinatorics "0.1.6"]]
  :repl-options {:init-ns net.paradigmx.horizon.play24}
  :main ^{:skip-aot true} net.paradigmx.horizon.play24)
