package com.main.poller;

import com.main.service.Service;
import com.main.service.Status;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BackgroundPoller {

  private Logger logger = LoggerFactory.getLogger(BackgroundPoller.class);

  private WebClient webClient;

  public BackgroundPoller(WebClient webClient) {
    this.webClient = webClient;
  }

  public Future<List<String>> pollServices(Map<String, Service> services) {
    List<String> okServices = new CopyOnWriteArrayList<>();
    Future<List<String>> okServicesFuture = Future.future();
    if (services.isEmpty()) {
      Future.failedFuture("No services to poll");
    }
    List<Future> servicesFutures = getServicesFuture(services);
    CompositeFuture.join(servicesFutures)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                servicesFutures.forEach(
                    service -> {
                      if (service.result() != null) {
                        okServices.add(service.result().toString());
                      }
                    });
                okServicesFuture.complete(okServices);
              }
            });

    return okServicesFuture;
  }

  private List<Future> getServicesFuture(Map<String, Service> services) {
    return services.entrySet().stream()
        .map(
            entry -> {
              Future serviceFuture = Future.future();
              Service service = entry.getValue();
              webClient
                  .getAbs(entry.getValue().getUrl())
                  .timeout(1000)
                  .send(
                      ar -> {
                        if (ar.succeeded()) {
                          if (ar.result().statusCode() == HttpResponseStatus.OK.code()) {
                            service.setStatus(Status.OK);
                            services.put(entry.getKey(), service);
                            serviceFuture.complete(entry.getKey());
                          }
                        } else {
                          service.setStatus(Status.FAIL);
                          services.put(entry.getKey(), service);
                          logger.info(service.getName() + " " + Status.FAIL);
                          serviceFuture.complete();
                        }
                      });
              return serviceFuture;
            })
        .collect(Collectors.toList());
  }
}
