/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.unpack.client;

import com.netty.unpack.model.User;
import com.netty.unpack.protocol.Request;
import com.netty.unpack.protocol.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ClientHandler extends SimpleChannelInboundHandler<Response> {

    private int count;

    /**
     * 通道注册
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    /**
     * 服务器的连接被建立后调用
     * 建立连接后该 channelActive() 方法被调用一次
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        User user = new User();
        user.setUsername("测试客户端");
        user.setPassword("4567");
        user.setAge(21);

        Request request = new Request();
        request.setRequestId(3L);
        request.setParameter(user);

        //当被通知该 channel 是活动的时候就发送信息
        ctx.writeAndFlush(request);

        // 使用客户端发送 10 条数据 hello,server 编号
        //for(int i= 0; i< 10; ++i) {
        //    ByteBuf buffer = Unpooled.copiedBuffer("hello,server " + i, Charset.forName("utf-8"));
        //    ctx.writeAndFlush(buffer);
        //}
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
        System.out.println("服务器发来消息 : " + response);

        //ByteBuf msg = null;
        //byte[] buffer = new byte[msg.readableBytes()];
        //msg.readBytes(buffer);
        //
        //String message = new String(buffer, Charset.forName("utf-8"));
        //System.out.println("客户端接收到消息=" + message);
        //System.out.println("客户端接收消息数量=" + (++this.count));
    }

    /**
     * 捕获异常时调用
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        //记录错误日志并关闭 channel
        cause.printStackTrace();
        ctx.close();
    }

}