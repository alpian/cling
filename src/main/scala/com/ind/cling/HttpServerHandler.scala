package com.ind.cling

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.cookie.{ServerCookieEncoder, ServerCookieDecoder}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpContent, HttpHeaders, HttpObject, HttpRequest, LastHttpContent, QueryStringDecoder}
import io.netty.util.CharsetUtil

import scala.collection.JavaConversions._


class HttpServerHandler(var request: HttpRequest) extends SimpleChannelInboundHandler[HttpObject] {

  /** Buffer that stores the response content */
  val buf = new StringBuilder()

  override def channelReadComplete(ctx: ChannelHandlerContext) = {
    ctx.flush()
  }

  private def handleRequest(request: HttpRequest) = {
    if (HttpHeaders.is100ContinueExpected(request)) {
      send100Continue(ctx)
    }

    buf.setLength(0);
    buf.append("WELCOME TO Cling WEB SERVER\r\n")
    buf.append("===================================\r\n")
    buf.append("VERSION: ").append(request.getProtocolVersion).append("\r\n")
    buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n")
    buf.append("REQUEST_URI: ").append(request.getUri).append("\r\n\r\n")

    val headers = request.headers()
    if (!headers.isEmpty()) {

      for (h <- headers) {
        val key = h.getKey()
        val value = h.getValue()
        buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n")
      }
      buf.append("\r\n")
    }

    val queryStringDecoder = new QueryStringDecoder(request.getUri)
    val params = queryStringDecoder.parameters()
    if (!params.isEmpty) {
      for (p <- params.entrySet()) {
        val key = p.getKey
        val vals = p.getValue
        for (vall <- vals) {
          buf.append("PARAM: ").append(key).append(" = ").append(vall).append("\r\n")
        }
      }
      buf.append("\r\n")
    }

    appendDecoderResult(buf, request)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) = {
    msg match {
      case request: HttpRequest =>
        handleRequest(request)
      case httpContent: HttpContent =>
        val content = httpContent.content()
        if (content.isReadable()) {
          buf.append("CONTENT: ")
          buf.append(content.toString(CharsetUtil.UTF_8))
          buf.append("\r\n")
          appendDecoderResult(buf, request)
        }

        if (msg.isInstanceOf[LastHttpContent]) {
          buf.append("END OF CONTENT\r\n")

          val trailer = msg.asInstanceOf[LastHttpContent]
          if (!trailer.trailingHeaders().isEmpty()) {
            buf.append("\r\n")
            for (name <- trailer.trailingHeaders().names()) {
              for (value <- trailer.trailingHeaders().getAll(name)) {
                buf.append("TRAILING HEADER: ")
                buf.append(name).append(" = ").append(value).append("\r\n")
              }
            }
            buf.append("\r\n")
          }

          if (!writeResponse(trailer, ctx)) {
            // If keep-alive is off, close the connection once the content is fully written.
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
          }
        }
    }
  }

  def appendDecoderResult(buf: StringBuilder, o: HttpObject) = {
    val result = o.getDecoderResult()
    if (!result.isSuccess()) {
      buf.append(".. WITH DECODER FAILURE: ")
      buf.append(result.cause())
      buf.append("\r\n")
    }
  }

  def writeResponse(currentObj: HttpObject, ctx: ChannelHandlerContext) = {
    // Decide whether to close the connection or not.
    val keepAlive = HttpHeaders.isKeepAlive(request)
    // Build the response object.
    val response = new DefaultFullHttpResponse(
      HTTP_1_1, if (currentObj.getDecoderResult().isSuccess()) OK else BAD_REQUEST,
      Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8))

    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8")

    if (keepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
      // Add keep alive header as per:
      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Encode the cookie.
    val cookieString = request.headers().get(COOKIE)
    if (cookieString != null) {
      val cookies = ServerCookieDecoder.STRICT.decode(cookieString)
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        for (cookie <- cookies) {
          response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie))
        }
      }
    } else {
      // Browser sent no cookie.  Add some.
      response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode("key1", "value1"))
      response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode("key2", "value2"))
    }

    // Write the response.
    ctx.write(response)

    keepAlive
  }

  def send100Continue(ctx: ChannelHandlerContext) {
    val response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE)
    ctx.write(response)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    ctx.close()
  }
}