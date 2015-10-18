package com.ind.cling

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpRequestDecoder, HttpResponseEncoder}
import io.netty.util.concurrent.GenericFutureListener


class HttpServerInitializer extends ChannelInitializer[SocketChannel] {
    override def initChannel(clientChannel: SocketChannel) = {
        val p = clientChannel.pipeline()
        p.addLast(new HttpRequestDecoder())
        p.addLast(new HttpResponseEncoder())
        p.addLast(new HttpServerHandler(null))
    }
}


class HttpServer(port: Int) {
  def start() = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup((Runtime.getRuntime.availableProcessors() * 4) + 32)

    val b = new ServerBootstrap()
    b.option[Integer](ChannelOption.SO_BACKLOG, 1024)
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new HttpServerInitializer())

    b.bind(port).sync().channel().closeFuture().addListener(new GenericFutureListener[Nothing] {
      override def operationComplete(future: Nothing): Unit = {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
      }
    })
  }
}