package com.ind.cling

import com.typesafe.config.ConfigFactory
import io.undertow.Undertow
import io.undertow.server.{HttpServerExchange, HttpHandler}
import io.undertow.util.Headers
import org.slf4j.{LoggerFactory, Logger}

class HttpServer(port: Int) {
  def start() = {
    val server = Undertow.builder()
      .addHttpListener(port, "localhost")
      .setHandler(new HttpHandler() {
      def handleRequest(exchange: HttpServerExchange) = {
        if (exchange.isInIoThread) {
          exchange.dispatch(this)
        }
        exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
        exchange.getResponseSender.send("Hello World2")
      }
    }).build()
    server.start()
  }
}

object Launch {
  def main(args: Array[String]): Unit = {
    System.setProperty("org.jboss.logging.provider", "slf4j")
    System.setProperty("cling.http.server.port", "8080")
    val port = ConfigFactory.load().getInt("cling.http.server.port")
    val logger = LoggerFactory.getLogger(classOf[HttpServer])
    logger.info(s"Cling server is starting on port $port...")
    new HttpServer(port).start()
    logger.info(s"Cling server listening on port $port")
  }
}
