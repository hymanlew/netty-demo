/**
 * uifuture.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.uifuture.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * 处理服务器端通道
 * ChannelInboundHandlerAdapter 继承自 ChannelHandlerAdapter，实现了 ChannelInboundHandler 接口。
 * ChannelInboundHandler 接口提供了不同的事件处理方法，可进行重写，实现了服务器的业务逻辑，决定了连接创建后和接收到信息后该如何处理。
 *
 * Sharable 注解，标识这类的实例可以在 channel 之间共享。
 */
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 每个信息入站时都会调用，接收数据并进行处理。
     * 事件处理方法。每当从客户端接收到新数据时，使用该方法来接收客户端的消息。在此示例中，接收到的消息的类型为 ByteBuf。
     *
     * ChannelHandlerContext ctx：上下文对象，含有管道 pipeline，通道 channel，连接地址等等，包含很多属性信息
     * Object msg：客户端发送的数据，默认为 Object
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        // 比如这里有一个非常耗时的业务 -> 异步执行 -> 提交给该 channel 对应的 NIOEventLoop 的 taskQueue 任务队列中，防止 pipeline 阻塞
        // 解决方案 1，用户程序自定义的普通任务
        ctx.channel().eventLoop().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));
                    System.out.println("channel code=" + ctx.channel().hashCode());

                } catch (Exception ex) {
                    System.out.println("发生异常" + ex.getMessage());
                }
            }
        });

        // 这个任务的执行时机，是本任务 sleep 加上一个任务 sleep 的时间之后。因为它们两个是由同一个线程处理的，是放到了同一个队列中
        ctx.channel().eventLoop().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵3", CharsetUtil.UTF_8));
                    System.out.println("channel code=" + ctx.channel().hashCode());

                } catch (Exception ex) {
                    System.out.println("发生异常" + ex.getMessage());
                }
            }
        });

        //解决方案2，用户自定义定时任务，该任务是提交到 scheduleTaskQueue 中。并且也是会先执行之前的任务，因为它们是处于同一线程中
        ctx.channel().eventLoop().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵4", CharsetUtil.UTF_8));
                    System.out.println("channel code=" + ctx.channel().hashCode());

                } catch (Exception ex) {
                    System.out.println("发生异常" + ex.getMessage());
                }
            }
        }, 5, TimeUnit.SECONDS);

        System.out.println("go on ...");


        /**
         * 查询是 workerGroup 下第几个线程处理的请求，每一个客户端都依次对应到 workerGroup 中的每个线程，如果全部对应完之后，
         * 就会从第一个 workerGroup 的线程再依次对应。默认子线程（NioEventLoop）的个数，是实际 cpu 核数 * 2.
         */
        System.out.println("服务器读取线程 " + Thread.currentThread().getName() + " channle =" + ctx.channel());
        System.out.println("server ctx =" + ctx);

        /**
         * Java NIO 管道是2个线程之间的单向数据连接。Pipe 有一个 source 通道和一个 sink 通道。数据会被写到 sink 通道，从 source
         * 通道读取。管道，它可以关联一个或多个 handler 处理器，数据在经过管道时就可以同时进行相关的业务处理。
         * 而通道更多的是指数据的传输，读或写的操作。
         */
        System.out.println("看看channel 和 pipeline的关系，pipeline 本质上是一个双向链接, 出站入站");
        Channel channel = ctx.channel();
        ChannelPipeline pipeline = ctx.pipeline();


        //将 msg 转成一个 ByteBuf，注意此 ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer
        ByteBuf in = (ByteBuf) msg;
        System.out.println("客户端发来消息: " + in.toString(CharsetUtil.UTF_8));
        System.out.println("客户端地址:" + channel.remoteAddress());

        /**
         * ChannelHandlerContext 提供各种不同的操作，用于触发不同的 I/O 时间和操作。
         * 调用 write 方法来逐字返回服务器返回给客户端的信息，并且只调用 write 是不会释放资源的，它会缓存，直到调用 flush 方法。
         *
         * writeAndFlush 是 write + flush，是将服务器返回给客户端的数据写入到缓存，并刷新。一般需要对这个发送的数据进行编码，
         * 并冲刷到远程节点。关闭通道后，操作完成。并且 Netty 会在写的时候自动释放资源。
         */
        ctx.write(in);
    }

    /**
     * 当前读操作读取完最后一个消息，即最后一个消息被 channelRead() 方法消费时调用。如果 ChannelOption.AUTO_READ 属性被设置为off,
     * 则不会再尝试从当前 channel 中读取 inbound 入站数据，直到 ChannelHandlerContext.read() 方法被调用.
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("channel 通道读取完成");

        // 第一种写法，写一个空的 buf，并刷新写出区域。完成后关闭 sockchannel 连接。
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);

        // 第二种方法：在 client 端关闭 channel 连接，这样的话，会触发两次 channelReadComplete 方法。
        //ctx.flush();

        // 第三种：改成为这种写法也可以，但是这种写法，没有第一种方法好。
        //ctx.flush().close().sync();
    }

    /**
     * 读操作时捕获到异常时调用，一般是需要关闭通道
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        //打印异常堆栈跟踪
        cause.printStackTrace();
        //关闭通道
        ctx.close();
    }
}