# Horizon

The Paradigm X API project.

## Getting Started

1. Start the application: `lein run`
2. Go to [localhost:8080](http://localhost:8080/) to see: `Hello World!`
3. Read your app's source code at `src/horizon/service.clj`. Explore the docs of functions
   that define routes and responses.
4. Run your app's tests with `lein test`. Read the tests at `test/horizon/service_test.clj`.

## Configuration

To configure logging see `config/logback.xml`. By default, the app logs to `stdout` and `logs/`.

## Developing your service

1. Start a new REPL: `lein repl`
2. Start your service in dev-mode: `(def dev-serv (run-dev))`
3. Connect your editor to the running REPL session.
   Re-evaluated code will be immediately seen in the service.
