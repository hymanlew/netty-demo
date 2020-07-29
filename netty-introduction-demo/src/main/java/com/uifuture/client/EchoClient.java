/**
 * uifuture.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.uifuture.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * Channel：
 * 是 Netty 网络通信的组件，能够用于执行网络 I/O 操作。通过 Channel 可获得当前网络连接的通道的状态，及网络连接的配置参数（例如接收缓冲区大小）。
 * Channel 提供异步的网络 I/O 操作(如建立连接，读写，绑定端口)，异步调用意味着任何 I/O 调用都将立即返回，并且不保证在调用结束时所请求的 I/O 操作
 * 已完成。调用会立即返回一个 ChannelFuture 实例，通过注册监听器到 ChannelFuture 上，可以在 I/O 操作成功、失败或取消时回调通知调用方，并且支持
 * 关联 I/O 操作与对应的处理程序。不同协议、不同阻塞类型的连接都有不同的 Channel 类型与之对应。
 *
 * 常用的 Channel 类型有：
 * NioSocketChannel，异步的客户端 TCP Socket 连接。
 * NioServerSocketChannel，异步的服务器端 TCP Socket 连接。
 * NioDatagramChannel，异步的 UDP 连接。
 * NioSctpChannel，异步的客户端 Sctp 连接。
 * NioSctpServerChannel，异步的 Sctp 服务器端连接，这些通道涵盖了 UDP 和 TCP 网络 IO 以及文件 IO。
 *
 * Java NIO 管道是2个线程之间的单向数据连接。Pipe有一个source通道和一个sink通道。数据会被写到sink通道，从source通道读取。
 * 管道，它可以关联一个或多个 handler 处理器，数据在经过管道时就可以同时进行相关的业务处理。而通道更多的是指数据的传输，读或写的操作。
 *
 * Netty 中的通道是对 java 原生网络编程 BIO、NIO api 的封装，其顶级接口是 Channel。以 TCP 编程为例，在java中，有两种方式：
 * 1，基于BIO，JDK1.4之前，通常使用 java.net 包中的 ServerSocket 和 Socket 来代表服务端和客户端。
 * 2，基于NIO，JDK1.4 引入nio编程之后，使用 java.nio.channels 包中的 ServerSocketChannel 和 SocketChannel 来代表服务端与客户端。
 * 3，使用 OioServerSocketChannel，OioSocketChannel 对 java.net 包中的 ServerSocket 与 Socket 进行了封装。
 * 4，使用 NioServerSocketChannel，NioSocketChannel 对 java.nio.channels 包中的 ServerSocketChannel 和 SocketChannel 进行了封装。
 *
 * ChannelConfig：
 * 在Netty中，每种 Channel 都有对应的配置，用 ChannelConfig 来表示，它是一个接口，每个特定的 Channel 实现类都有自己对应的 ChannelConfig
 * 实现类，如：
 * NioSocketChannel 的对应的配置类为 NioSocketChannelConfig。
 * NioServerSocketChannel 的对应的配置类为 NioServerSocketChannelConfig。
 *
 * 在 Channel 接口中定义了一个方法 config()，用于获取特定通道实现的配置，子类需要实现这个接口。
 *
 *
 * 通过 host 和 port 连接服务器
 */
public class EchoClient {

    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        final String host = "127.0.0.1";
        final int port = 8081;

        new EchoClient(host, port).start();
    }

    public void start() throws Exception {

        // 客户端需要一个事件循环组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 创建客户端启动对象
            Bootstrap bootstrap = new Bootstrap();

            // 指定 EventLoopGroup 来处理客户端事件。由于我们使用 NIO 传输，所以用到了 NioEventLoopGroup 的实现
            bootstrap.group(group)

                    // 设置客户端通道的实现类(反射)
                    .channel(NioSocketChannel.class)

                    //设置服务器的地址和端口
                    .remoteAddress(new InetSocketAddress(host, port))

                    // 当建立一个连接和一个新的通道时，创建添加 EchoClientHandler 实例到 channel pipeline
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 加入自己的处理器
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });

            System.out.println("客户端 ok..");

            // 启动客户端连接服务器，等待连接完成。也可以在这里设置服务器地址和端口
            ChannelFuture future = bootstrap.connect().sync();

            // 阻塞直到 Channel 关闭，监听通道关闭
            future.channel().closeFuture().sync();

        } finally {

            //调用 shutdownGracefully() 来关闭线程池和释放所有资源
            group.shutdownGracefully().sync();
        }
    }
}