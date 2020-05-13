# Vertx polling services

The application consists of a backend service written in Vert.x (https://vertx.io/) that keeps a list of services (defined by a URL), and periodically does a HTTP GET to each and saves the response ("OK" or "FAIL").

Every service has it's own URL, name, and status. We have to poll every service and get the status of each of them using polling.

CRUD Operations:-

1. Create new sericve
2. Get all the services and make sure services do not disappear after server is restarted.
3. Delete any service you want with it's name.

All opertions should be done asynchronously using AsyncHandlers.

# Building
```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```
# run
Run DBMigration.java to create a schema on your local.
You can run the application on localhost:8080 and perform crud operation on services and see status of service after refresh.

You can also run gradle directly from the command line:
```
./gradlew clean run
```
