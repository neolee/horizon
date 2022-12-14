(ns net.paradigmx.common.core)

;; if-let multiple bindings version
;; bought from https://clojuredocs.org/clojure.core/if-let
;; NOTE `else` branch not work with expressions
(defmacro if-let*
  "Like `core/if-let` but support multiple bindings for `then` iff all tests
  are truthy."
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

;; let `args` in `-main` function support both repl and cli env
(defn parse-int-arg
  "parse `arg` to int if it is string (normally from `-main` being called in command line)"
  [arg]
  (if (= java.lang.String (type arg))
    (Integer/parseInt arg)
    arg))
