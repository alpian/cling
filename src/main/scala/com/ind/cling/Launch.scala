package com.ind.cling

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory


object Launch {
  def main(args: Array[String]): Unit = {
    System.setProperty("cling.http.server.port", "8080")
    val port = ConfigFactory.load().getInt("cling.http.server.port")
    val logger = LoggerFactory.getLogger(classOf[HttpServer])
    logger.info(s"Cling server is starting on port $port...")
    new HttpServer(port).start()
    logger.info(s"Cling server listening on port $port")
  }
}
