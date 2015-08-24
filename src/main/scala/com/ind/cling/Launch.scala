package com.ind.cling

import com.typesafe.config.ConfigFactory

class HttpServer(port: Int) {
  def start() = {
  }
}

object Launch {
  def main(args: Array[String]): Unit = {
    val port = ConfigFactory.load().getInt("cling.http.server.port")
    println(s"Cling server starting on port $port...")
    new HttpServer(port).start()
  }
}
