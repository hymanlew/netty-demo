package com.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

/**
 * 1. SimpleChannelInboundHandler 继承自 ChannelInboundHandlerAdapter
 * 2. HttpObject 是客户端和服务器端相互通讯的数据，被封装成了 HttpObject
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    /**
     * 读取客户端数据
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        System.out.println(
                "对应的 channel=" + ctx.channel() +
                " pipeline =" + ctx.pipeline() +
                " 通过pipeline获取channel" + ctx.pipeline().channel() +
                " 当前 ctx 的 handler =" + ctx.handler());

        //判断 msg 是不是 httprequest请求
        if(msg instanceof HttpRequest) {

            System.out.println("ctx 类型 = " + ctx.getClass());
            System.out.println("pipeline hashcode" + ctx.pipeline().hashCode() + " HttpServerHandler hash = " + this.hashCode());
            System.out.println("msg 类型 = " + msg.getClass());
            System.out.println("客户端地址 = " + ctx.channel().remoteAddress());

            //获取uri, 过滤指定的资源
            HttpRequest httpRequest = (HttpRequest) msg;
            URI uri = new URI(httpRequest.uri());

            if("/favicon.ico".equals(uri.getPath())) {
                System.out.println("请求了 favicon.ico, 不做响应");
                return;
            }

            //回复信息给浏览器 http协议
            ByteBuf content = Unpooled.copiedBuffer("hello, 我是服务器", CharsetUtil.UTF_8);

            //构造一个http的相应，即 httpresponse
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            //将构建好 response 返回
            ctx.writeAndFlush(response);
        }
    }

}
