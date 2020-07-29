/**
 * uifuture.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.uifuture.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * ChannelHandler 及其实现类：
 * 在 nio编程中，我们经常需要对 channel 的输入和输出事件进行处理，Netty抽象出一个 ChannelHandler 概念，专门用于处理此类事件。因为IO事件分
 * 为输入和输出，因此 ChannelHandler 又具体分为 ChannelInboundHandler（处理入站 I/O 事件）和 ChannelOutboundHandler（处理出站 I/O 操作）。
 * ChannelHandlerAdapter、ChannelInboundHandlerAdapter 、ChannelOutboundHandlerAdapter 是 Netty 提供的适配器，对于输入输出事件，只需要
 * 继承适配器，重写感兴趣的方法即可。ChannelDuplexHandler 用于处理入站和出站事件。
 *
 * 在处理 channel 的 IO 事件时，通常会分成几个阶段。以读取数据为例，处理顺序是：处理半包或者粘包问题 --> 数据的解码（或者说是反序列化）-->
 * 数据的业务处理。不同的阶段执行不同的功能，因此通常会编写多个 ChannelHandler，来实现不同的功能。而且多个ChannelHandler 之间的顺序不能颠倒，
 * 例如必须先处理粘包解包问题，之后才能进行数据的业务处理。
 * ChannelHandler 是一个接口，处理 I/O 事件或拦截 I/O 操作，并将其转发到其 ChannelPipeline（业务处理链）中的下一个处理程序。其本身并没有提
 * 供很多方法，因为这个接口有许多的方法需要实现，为了方便使用可以继承它的子类，并重写对应的方法即可。
 *
 * ChannelHandlerContext：
 * 它是保存 Channel 相关的所有上下文信息，同时关联一个 ChannelHandler 对象。即 ChannelHandlerContext 中包含一个具体的事件处理器 ChannelHandler，
 * 同时也绑定了对应的 pipeline 和 Channel 的信息，方便对 ChannelHandler 进行调用。在 ChannelPipeline 中按照顺序添加 ChannelHandler，并在之
 * 后按照顺序调用。事实上每个 ChannelHandler 会被先封装成 ChannelHandlerContext。之后再封装进 ChannelPipeline 中。ChannelHandlerContext 的
 * 默认实现类是 DefaultChannelHandlerContext。
 * DefaultChannelPipeline.addLast(EventExecutorGroup, String,ChannelHandler)，它就是先将 ChannelHandler 当做参数构建成一个 DefaultChannelHandlerContext
 * 实例之后，再调用 addLast0() 方法维护 ChannelHandlerContext 的先后关系，从而确定了 ChannelHandler 的先后关系。
 */
@ChannelHandler.Sharable
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 服务器的连接被建立后调用，只调用一次
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("client " + ctx);

        // 当被通知该 channel 是活动的时候就发送信息
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello server: (>^ω^<)喵!" + System.currentTimeMillis(), CharsetUtil.UTF_8));
    }

    /**
     * 当从服务器接收到数据，即当通道有读取事件时，调用
     *
     * @param ctx
     * @param in
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {

        System.out.println("服务器发来消息: " + in.toString(CharsetUtil.UTF_8));
        System.out.println("服务器的地址： "+ ctx.channel().remoteAddress());
    }

    /**
     * 捕获异常时调用
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        //记录错误日志并关闭 channel
        cause.printStackTrace();
        ctx.close();
    }
}
