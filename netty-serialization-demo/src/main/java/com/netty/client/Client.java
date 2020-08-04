/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.client;

import com.netty.fst.protocol.Request;
import com.netty.fst.protocol.Response;
import com.netty.fst.protocol.TinyDecoder;
import com.netty.fst.protocol.TinyEncoder;
import com.netty.protobuf.StudentPOJO;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.InetSocketAddress;

public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        final String host = "127.0.0.1";
        final int port = 8081;

        new Client(host, port).start();
    }

    public void start() throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))

                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {

                            ch.pipeline()
                                    // 添加编解码. 发送自定义的类型, 而Handler的方法接收的msg参数的实际类型也是相应的自定义类了
                                    .addLast(new TinyEncoder(Request.class))
                                    .addLast(new TinyDecoder(Response.class))
                                    .addLast(new ClientHandler());

                            // 在 pipeline 中加入 ProtoBufEncoder
                            ch.pipeline()
                                    .addLast("encoder", new ProtobufEncoder())
                                    .addLast("decoder", new ProtobufDecoder(StudentPOJO.Student.getDefaultInstance()))
                                    .addLast(new ProtoClientHandler());
                        }
                    });

            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}