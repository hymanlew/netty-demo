/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.hearbest.server;

import com.netty.hearbest.codec.TinyDecoder;
import com.netty.hearbest.codec.TinyEncoder;
import com.netty.hearbest.model.RequestInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class Server {

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = 8081;
        new Server(port).start();
    }

    public void start() throws Exception {

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)

                    // 添加一个日志处理器
                    .handler(new LoggingHandler(LogLevel.INFO))

                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline
                                    // 添加编解码. 发送自定义的类型, 而Handler的方法接收的msg参数的实际类型也是相应的自定义类了
                                    .addLast(new TinyDecoder(RequestInfo.class))
                                    .addLast(new TinyEncoder(RequestInfo.class))
                                    .addLast(new ServerHandler());

                            /**
                             * 加入一个 netty 提供 IdleStateHandler
                             *
                             * 1，IdleStateHandler：   是 netty 提供的处理空闲状态的处理器
                             * 2，long readerIdleTime：表示多长时间没有读，就会发送一个心跳检测包检测是否连接
                             * 3，long writerIdleTime：表示多长时间没有写, 就会发送一个心跳检测包检测是否连接
                             * 4，long allIdleTime：   表示多长时间没有读写, 就会发送一个心跳检测包检测是否连接
                             *
                             * 当 IdleStateEvent 触发后，就会传递给管道的下一个 handler 去处理。通过调用(触发)下一个 handler
                             * 的 userEventTiggered, 在该方法中去处理 IdleStateEvent(读空闲，写空闲，读写空闲) 相应的操作。
                             */
                            pipeline.addLast(new IdleStateHandler(3,5,10, TimeUnit.SECONDS));

                            // 加入一个对空闲检测后，进一步处理的 handler(自定义)
                            pipeline.addLast(new ServerHandler());
                        }
                    })

                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = b.bind(port).sync();
            System.out.println(Server.class.getName() + " started and listen on " + future.channel().localAddress());

            future.channel().closeFuture().sync();

        } finally {
            // 释放 channel 和 块，直到它被关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}