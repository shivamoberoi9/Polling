package com.main;

import com.main.service.MainVerticle;
import io.vertx.core.Vertx;

public class Start {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }
}
