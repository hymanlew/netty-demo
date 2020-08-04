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
        response.setRequestId(2L);
        response.setError("success");
        User user = new User();
        user.setUsername("测试");
        user.setPassword("1234");
        user.setAge(21);
        response.setResult(user);
        //addListener是非阻塞的，异步执行。它会把特定的ChannelFutureListener添加到ChannelFuture中，然后I/O线程会在I/O操作相关的future完成的时候通知监听器。
        ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture ->
                System.out.println("接口响应:" + request.getRequestId())
        );
    }
}