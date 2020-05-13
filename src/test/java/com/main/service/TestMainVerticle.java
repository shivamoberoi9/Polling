package com.main.service;

import com.main.poller.BackgroundPoller;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(
        new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
    add_services(vertx, testContext);
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(200, response.result().statusCode());
                      JsonArray body = response.result().bodyAsJsonArray();
                      assertEquals(1, body.size());
                      testContext.completeNow();
                    }));
  }

  @Test
  @DisplayName("Add a service with Name and URL")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void add_services(Vertx vertx, VertxTestContext testContext) {
    JsonObject jsonBody = new JsonObject();
    jsonBody.put("name", "yahoo");
    jsonBody.put("url", "http://yahoo.com");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .sendJson(
            jsonBody,
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(201, response.result().statusCode());
                      testContext.completeNow();
                    }));
  }

  @Test
  @DisplayName("Add a service with invalid URL")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void add_unknown_service(Vertx vertx, VertxTestContext testContext) {
    final JsonObject jsonBody = new JsonObject();
    jsonBody.put("name", "yahoo");
    jsonBody.put("url", "abcd");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .sendJson(
            jsonBody,
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(400, response.result().statusCode());
                      testContext.completeNow();
                    }));
  }

  @Test
  @DisplayName("Delete a Service with it's name")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void delete_service(Vertx vertx, VertxTestContext testContext) {
    JsonObject jsonBody = new JsonObject();
    jsonBody.put("name", "yahoo");
    jsonBody.put("url", "http://yahoo.com");
    WebClient webClient = WebClient.create(vertx);
    webClient.post(8080, "::1", "/service").sendJson(jsonBody, response -> {});
    webClient
        .delete(8080, "::1", "/service")
        .putHeader("content-type", "application/json")
        .setQueryParam("name", "yahoo")
        .send(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(200, response.result().statusCode());
                      testContext.completeNow();
                    }));
  }

  @Test
  @DisplayName("Delete a Service with it's wrong name")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void delete_unknown_service(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .delete(8080, "::1", "/service")
        .putHeader("content-type", "application/json")
        .setQueryParam("name", "yahoo1")
        .send(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(404, response.result().statusCode());
                      testContext.completeNow();
                    }));
  }

  @Test
  @DisplayName("Poll services and check list of OK services")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void poll_services(Vertx vertx, VertxTestContext testContext) {
    WebClientOptions webClientOptions = new WebClientOptions().setKeepAlive(true);
    WebClient client = WebClient.create(vertx, webClientOptions);
    BackgroundPoller backgroundPoller = new BackgroundPoller(client);
    Service yahooService = new Service();
    yahooService.setUrl("http://www.yahoo.com");
    Service stackOverflowService = new Service();
    // setting to incorrect URL to get false status for this service
    stackOverflowService.setUrl("http://www.stackoverflow1.com");
    Map<String, Service> services = new HashMap<>();
    services.put(yahooService.getUrl(), yahooService);
    services.put(stackOverflowService.getUrl(), stackOverflowService);
    backgroundPoller
        .pollServices(services)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(1, ar.result().size(), "One service is returned as OK");
                testContext.completeNow();
              }
            });
  }
}
