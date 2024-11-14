(ns net.paradigmx.common.core)

;; `when-let` and `if-let` multiple bindings version
;; bought from https://gist.github.com/mbuczko/b7656ed5ae56602b41e2dda5a417cf1a
;; NOTE `else` branch not work with expressions
(defmacro assert-all
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-all more)))))

(defmacro when-let*
  "Like `core/when-let` but support multiple bindings for `then` iff all tests
  are truthy."
  [bindings & body]
  (when (seq bindings)
    (assert-all
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "exactly even forms in binding vector"))
  (if (seq bindings)
    `(when-let [~(first bindings) ~(second bindings)]
       (when-let* ~(vec (drop 2 bindings)) ~@body))
    `(do ~@body)))

(defmacro if-let*
  "Like `core/if-let` but support multiple bindings for `then` iff all tests
  are truthy."
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (when (seq bindings)
     (assert-all
      (vector? bindings) "a vector for its binding"
      (even? (count bindings)) "exactly even forms in binding vector"))
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(vec (drop 2 bindings)) ~then ~else)
        ~(if-not (second bindings) else nil))
     then)))

;; let `args` in `-main` function support both repl and cli env
(defn parse-int-arg
  "parse `arg` to int if it is string (normally from `-main` being called in command line)"
  [arg]
  (if (= java.lang.String (type arg))
    (Integer/parseInt arg)
    arg))
