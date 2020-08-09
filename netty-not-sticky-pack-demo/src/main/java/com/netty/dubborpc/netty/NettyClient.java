package com.netty.dubborpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyClient {

    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static NettyClientHandler client;
    private int count = 0;

    /**
     * 编写方法使用代理模式，获取一个代理对象
     *
     * @param serivceClass
     * @param protocol
     * @return
     */
    public Object getBean(final Class<?> serivceClass, final String protocol) {

        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{serivceClass}, (proxy, method, args) -> {

                    System.out.println("(proxy, method, args) 进入...." + (++count) + " 次");

                    // 客户端每通过代理对象，调用一次 helloservice 方法, 就会进入到该 {} 部分的代码
                    if (client == null) {
                        initClient();
                    }

                    //设置要发给服务器端的信息，providerName 为协议头，args[0] 为客户端调用 api hello(???) 的参数
                    client.setPara(protocol + args[0]);
                    return executor.submit(client).get();
                });
    }

    /**
     * 初始化客户端
     */
    private static void initClient() {

        client = new NettyClientHandler();

        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)

                // 设置为不延迟
                .option(ChannelOption.TCP_NODELAY, true)

                .handler(
                        new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new StringDecoder());
                                pipeline.addLast(new StringEncoder());
                                pipeline.addLast(client);
                            }
                        }
                );

        try {
            bootstrap.connect("127.0.0.1", 7000).sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
