package com.netty.longconnect;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Http 协议是无状态的, 浏览器和服务器间的请求响应一次，下一次会重新创建连接。
 *
 * Netty 通过 WebSocket 编程实现服务器和客户端长连接：
 * 要求：实现基于 webSocket 的长连接的全双工的交互，改变 Http 协议多次请求的约束，实现长连接，服务器可以发送消息给浏览器。客户端
 * 浏览器和服务器端会相互感知，比如服务器关闭了，浏览器会感知，同样浏览器关闭了，服务器会感知。
 *
 */
public class Server {

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = 8081;
        new com.netty.hearbest.server.Server(port).start();
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

                            // 因为是基于 http 协议，所以使用 http 的编码和解码器
                            pipeline.addLast(new HttpServerCodec());

                            // 以块方式写，添加 ChunkedWriteHandler 处理器
                            pipeline.addLast(new ChunkedWriteHandler());

                            /**
                             * 1，http 数据在传输过程中是分段的, HttpObjectAggregator 就是可以将多个段聚合。
                             * 2，这就就是为什么，当浏览器发送大量数据时，就会发出多次 http 请求。
                             */
                            pipeline.addLast(new HttpObjectAggregator(8192));

                            /**
                             * 1. 对于 websocket 协议，它的数据是以帧（frame）形式传递的
                             * 2. 可以看到 WebSocketFrame 下面有六个子类（文本传输，二进制传输等等）
                             * 3. 浏览器请求时 ws://localhost:7000/ws 表示请求的 uri
                             * 4. WebSocketServerProtocolHandler 核心功能是将 http 协议升级为 ws 协议, 以实现保持长连接。
                             * 是通过一个服务器状态码 101 来进行切换的。
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

                            // 自定义的 handler，处理业务逻辑
                            pipeline.addLast(new WsTextWebSocketFrameHandler());
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
