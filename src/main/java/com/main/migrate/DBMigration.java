package com.main.migrate;

import io.vertx.core.Vertx;
import com.main.service.DBConnector;

public class DBMigration {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DBConnector connector = new DBConnector(vertx);
    connector
        .query(
            "CREATE TABLE IF NOT EXISTS service (name VARCHAR(128), url VARCHAR(128) NOT NULL, Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)")
        .setHandler(
            done -> {
              if (done.succeeded()) {
                System.out.println("completed db migrations");
              } else {
                done.cause().printStackTrace();
              }
              vertx.close(shutdown -> System.exit(0));
            });
  }
}
