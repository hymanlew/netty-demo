/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.server;

import com.netty.fst.protocol.Request;
import com.netty.fst.protocol.Response;
import com.netty.fst.protocol.TinyDecoder;
import com.netty.fst.protocol.TinyEncoder;
import com.netty.protobuf.DataInfo;
import com.netty.protobuf.StudentPOJO;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

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

                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) {

                            ch.pipeline()
                                    // 添加编解码. 发送自定义的类型, 而Handler的方法接收的msg参数的实际类型也是相应的自定义类了
                                    .addLast(new TinyDecoder(Request.class))
                                    .addLast(new TinyEncoder(Response.class))
                                    .addLast(new FstServerHandler());

                            // 在 pipeline 中加入 ProtoBufEncoder
                            ch.pipeline()
                                    .addLast("decoder", new ProtobufDecoder(StudentPOJO.Student.getDefaultInstance()))
                                    .addLast("encoder", new ProtobufEncoder())
                                    .addLast(new ProtoSServerHandler());

                            ch.pipeline()
                                    .addLast("decoder", new ProtobufDecoder(DataInfo.DMessage.getDefaultInstance()))
                                    .addLast("encoder", new ProtobufEncoder())
                                    .addLast(new ProtoDServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            System.out.println(Server.class.getName() + " started and listen on " + f.channel().localAddress());

            f.channel().closeFuture().sync();

        } finally {
            //释放 channel 和 块，直到它被关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}