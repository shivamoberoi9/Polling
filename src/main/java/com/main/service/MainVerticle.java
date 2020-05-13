package com.main.service;

import com.main.poller.BackgroundPoller;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  private Map<String, Service> services = new ConcurrentHashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller;

  @Override
  public void start(Future<Void> startFuture) {
    WebClientOptions webClientOptions = new WebClientOptions().setKeepAlive(true);
    WebClient client = WebClient.create(vertx, webClientOptions);
    poller = new BackgroundPoller(client);
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    initServices();
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(
            8080,
            result -> {
              if (result.succeeded()) {
                logger.info("Application started");
                startFuture.complete();
              } else {
                startFuture.fail(result.cause());
              }
            });
  }

  private void initServices() {
    connector
        .query("select name, url from service;")
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                ar.result()
                    .getResults()
                    .forEach(
                        result -> {
                          Service service =
                              new Service(result.getString(0), result.getString(1), Status.UNKNOWN);

                          services.put(result.getString(0), service);
                        });
                logger.info("All Services initiated");
              }
            });
  }

  private void setRoutes(Router router) {
    router.route("/*").handler(StaticHandler.create());
    router
        .get("/service")
        .handler(
            req -> {
              List<JsonObject> jsonServices =
                  services.entrySet().stream()
                      .map(
                          service ->
                              new JsonObject()
                                  .put("name", service.getKey())
                                  .put("status", service.getValue().getStatus())
                                  .put("url", service.getValue().getUrl()))
                      .collect(Collectors.toList());
              req.response()
                  .putHeader("content-type", "application/json")
                  .end(new JsonArray(jsonServices).encode());
            });
    router
        .post("/service")
        .handler(
            req -> {
              JsonObject jsonBody = req.getBodyAsJson();
              Service service = jsonBody.mapTo(Service.class);
              String url = service.getUrl();
              String name = service.getName();
              service.setStatus(Status.UNKNOWN);
              if (name != null && !name.isEmpty() && isValidUrl(url)) {
                services.put(service.getName(), service);
                String insertSql = "insert into service(name, url)  values(?, ?);";
                JsonArray params = new JsonArray().add(name).add(url);
                connector.query(insertSql, params).setHandler(ar->{
                    if(ar.succeeded()){
                        req.response()
                                .putHeader("content-type", "text/plain")
                                .setStatusCode(HttpResponseStatus.CREATED.code())
                                .end(Status.OK);
                    }
                    else{
                        req.response()
                                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                                .end(Status.FAIL);
                    }
                });

              } else {
                req.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end(Status.FAIL);
              }
            });
    router
        .delete("/service")
        .handler(
            req -> {
              String name = req.request().getParam("name");
              if (name != null && !name.isEmpty() && services.containsKey(name)) {
                services.remove(name);
                String deleteSql = "delete from service where name = ?;";
                JsonArray params = new JsonArray().add(name);
                connector.query(deleteSql, params);
                req.response().putHeader("content-type", "text/plain").end(Status.OK);
              } else {
                req.response()
                    .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                    .putHeader("content-type", "text/plain")
                    .end(Status.FAIL);
              }
            });
  }

  private boolean isValidUrl(String url) {
    try {
      new URL(url).toURI();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
