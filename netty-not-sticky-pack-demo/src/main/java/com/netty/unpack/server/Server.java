/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.unpack.server;

import com.netty.unpack.protocol.Request;
import com.netty.unpack.protocol.Response;
import com.netty.unpack.protocol.TinyDecoder;
import com.netty.unpack.protocol.TinyEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

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

                    /**
                     * byteOrder：表示字节流表示的数据是大端还是小端，用于长度域的读取。默认 Netty 是大端序 ByteOrder.BIG_ENDIAN（可选）。
                     * maxFrameLength：表示的是包的最大长度，超出包的最大长度 netty将会报错。
                     * lengthFieldOffset：是指长度域（Length）的偏移量，表示跳过指定长度字节后才是长度域，也就是length前面的字节是头部信息。
                     * lengthFieldLength：记录该帧数据的长度（即记录的消息体字节数），表示长度域的长度。？？
                     * lengthAdjustment：该字段加长度字段等于数据帧的长度，表示包体长度调整的大小，长度域的数值表示的长度加上这个修正值表示的就是带header的包。
                     * initialBytesToStrip：从数据帧中跳过的字节数，表示获取完一个完整的数据包之后，忽略前面的指定的位数个字节，应用解码器拿到的就是不带长度域的数据包。
                     * failFast：如果为true，表示读取到长度域，如果TA的值的超过maxFrameLength，就抛出一个 TooLongFrameException。如果为false，表示只有当真正读取完长度域值的字节后，才抛出TooLongFrameException。默认为true，建议不要修改，否则可能会造成内存溢出。
                     */
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {

                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 0))
                                    // 添加编解码. 发送自定义的类型, 而Handler的方法接收的msg参数的实际类型也是相应的自定义类了
                                    .addLast(new TinyDecoder(Request.class))
                                    .addLast(new TinyEncoder(Response.class))
                                    .addLast(new ServerHandler());
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