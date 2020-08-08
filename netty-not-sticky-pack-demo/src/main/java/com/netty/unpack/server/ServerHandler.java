/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.unpack.server;

import com.netty.unpack.model.User;
import com.netty.unpack.protocol.Request;
import com.netty.unpack.protocol.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.UUID;

public class ServerHandler extends SimpleChannelInboundHandler<Request> {

    private int count;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {

        System.out.println("服务端接收到的消息 : " + request);

        User user = new User();
        user.setUsername("测试");
        user.setPassword("1234");
        user.setAge(21);

        Response response = new Response();
        response.setRequestId(2L);
        response.setResult(user);

        /**
         * addListener是非阻塞的，异步执行。它会把特定的 ChannelFutureListener 添加到 ChannelFuture 中，然后 I/O 线程会在 I/O
         * 操作相关的 future 完成时，通知监听器。
         */
        ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> System.out.println("接口响应:" + request.getRequestId())
        );


        //byte[] buffer = new byte[msg.readableBytes()];
        //ByteBuf msg = null;
        //msg.readBytes(buffer);
        //
        ////将buffer转成字符串
        //String message = new String(buffer, Charset.forName("utf-8"));
        //
        //System.out.println("服务器接收到数据 " + message);
        //System.out.println("服务器接收到消息量=" + (++this.count));
        //
        ////服务器回送数据给客户端, 回送一个随机id ,
        //ByteBuf responseByteBuf = Unpooled.copiedBuffer(UUID.randomUUID().toString() + " ", Charset.forName("utf-8"));
        //ctx.writeAndFlush(responseByteBuf);
    }
}