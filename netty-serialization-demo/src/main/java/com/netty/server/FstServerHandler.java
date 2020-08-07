/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.server;

import com.netty.fst.model.User;
import com.netty.fst.protocol.Request;
import com.netty.fst.protocol.Response;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FstServerHandler extends SimpleChannelInboundHandler<Request> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {

        System.out.println("服务端接收到的消息 : " + request);
        Response response = new Response();
        response.setRequestId(3L);
        response.setError("success");

        User user = new User();
        user.setUsername("hyman");
        user.setPassword("12333");
        user.setAge(21);
        response.setResult(user);

        /**
         * addListener 是非阻塞的，异步执行。它会把特定的 ChannelFutureListener 添加到 ChannelFuture 中，然后 I/O 线程会在 I/O
         * 操作相关的 future 完成的时候通知监听器。
         */
        ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture ->
                System.out.println("接口响应:" + request.getRequestId())
        );
    }
}