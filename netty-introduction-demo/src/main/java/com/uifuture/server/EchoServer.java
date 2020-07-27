/**
 * uifuture.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.uifuture.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;

/**
 * Netty模型（Netty 主要基于主从 Reactors 多线程模型（如图）做了一定的改进，其中主从 Reactor 多线程模型有多个 Reactor）。
 * 1，简单版：
 * BossGroup 线程维护 Selector，只关注 Accecpt，当接收到 Accept 事件，就自动获取到对应的 SocketChannel，并封装成 NIOScoketChannel
 * 同时注册到 Worker 线程（事件循环）进行维护。当 Worker 线程监听到 selector 中通道发生自己感兴趣的事件后，就交由 handler 进行处
 * 理（注意 handler 已经加入到通道中）。
 *
 * 2，进阶版：
 * Netty 抽象出两组线程池 BossGroup 专门负责接收客户端的连接，WorkerGroup 专门负责网络的读写，BossGroup 和 WorkerGroup 类型都是
 * NioEventLoopGroup。NioEventLoopGroup 相当于一个事件循环组，这个组中含有多个事件循环，每一个事件循环是 NioEventLoop。NioEventLoopGroup
 * 可以有多个线程，即可以含有多个 NioEventLoop。
 *
 * NioEventLoop 表示一个不断循环的执行处理任务的线程，每个 NioEventLoop 都有一个selector，一个 taskQueue，用于监听绑定在其上的
 * socket 的网络通讯。每个 Selector 上可以注册监听多个 NioChannel，每个 NioChannel 只会绑定在唯一的 NioEventLoop 上，每个 NioChannel
 * 都绑定有一个自己的 ChannelPipeline。
 *
 * 每个 Boss 的 NioEventLoop 循环执行的步骤有3步：轮询 accept 事件。处理 accept 事件，与 client 建立连接，生成 NioScocketChannel，
 * 并将其注册到某个 worker NIOEventLoop 上的 selector。处理任务队列的任务 ， 即 runAllTasks。
 *
 * 每个 Worker NioEventLoop 循环执行的步骤：轮询 read, write 事件。处理 i/o read，write 事件，在对应的 NioScocketChannel 中进行
 * 处理。处理任务队列的任务，即 runAllTasks。
 *
 * 每个 Worker NioEventLoop  处理业务时，会使用 pipeline（管道），pipeline 中包含了 channel，即通过 pipeline 可以获取到对应通道，
 * 另外管道中维护了很多的处理器。这些处理器在每个 NioEventLoop 内部采用串行化设计，从消息的读取 -> 解码 -> 处理 -> 编码 -> 发送，始
 * 终由 IO 线程 NioEventLoop 负责。
 *
 *
 * 配置服务器的启动代码。最少需要设置服务器绑定的端口，用来监听连接请求。
 */
public class EchoServer {

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        //设置端口值
        int port = 8081;
        //呼叫服务器的 start() 方法
        new EchoServer(port).start();
    }

    public void start() throws Exception {

        /**
         * Netty 内部都是通过线程在处理各种数据，EventLoopGroup 就是用来管理调度他们的，注册Channel，管理他们的生命周期。
         * NioEventLoopGroup 是一个处理I/O操作的多线程事件循环。
         * 创建两个线程组 bossGroup 和 workerGroup，两个都是无限循环。
         * bossGroup 和 workerGroup 含有的子线程（NioEventLoop）的个数，默认是实际 cpu 核数 NettyRuntime.availableProcessors() * 2
         */

        // bossGroup 作为 boss，接收传入连接。仅接收客户端连接请求，不做复杂的逻辑处理，为了尽可能减少资源的占用，取值越小越好
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // workerGroup 作为 worker，处理 boss 接收的真正的和客户端业务
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端的启动对象，配置参数，ServerBootstrap 负责建立服务端。也可以直接使用 Channel 去建立服务端，但是大多数情况下不使用这种方式
            ServerBootstrap b = new ServerBootstrap();

            // 使用链式编程来进行设置
            b.group(bossGroup, workerGroup)

                    // 指定使用 NioServerSocketChannel 产生一个Channel用来接收连接。NioServerSocketChannel 对应 TCP, NioDatagramChannel 对应 UDP
                    .channel(NioServerSocketChannel.class)

                    //设置 socket 地址使用所选的端口
                    .localAddress(new InetSocketAddress(port))

                    // 该 handler对应 bossGroup, childHandler 对应 workerGroup
                    //.handler(null)

                    // 创建一个通道初始化对象(匿名对象)，ChannelInitializer 配置一个新的 SocketChannel，用于向你的 Channel 中添加 ChannelInboundHandler 的实现
                    // 给 workerGroup 的 EventLoop 对应的 pipeline 管道设置处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) {

                            // 可以使用一个集合来统一管理 SocketChannel，在推送消息时，将业务加入到各个channel 对应的 NIOEventLoop 的 taskQueue 或者 scheduleTaskQueue 中即可
                            System.out.println("客户 socketchannel hashcode=" + ch.hashCode());

                            // 得到管道
                            ChannelPipeline pipeline = ch.pipeline();

                            // ChannelPipeline 用于存放管理 ChannelHandler，ChannelHandler 用于处理请求响应的业务逻辑相关代码
                            // 配置通信数据的处理逻辑，可以 addLast 多个
                            pipeline.addLast(new EchoServerHandler());

                            // 也可以加入一个 netty 提供的 httpServerCodec，即处理 http 的编解码器（codec = coder + decoder）
                            pipeline.addLast("MyHttpServerCodec",new HttpServerCodec());
                            // 增加一个自定义的 Httphandler，用于接收浏览器发送的 HTTP 请求
                            pipeline.addLast("MyHttpServerHandler", new HttpServerHandler());
                        }
                    })

                    /**
                     * 设置线程队列接收传入连接，并容纳等待连接的个数，设置 TCP 缓冲区
                     * BACKLOG 用于构造服务端套接字 ServerSocket 对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次
                     * 握手的请求的队列的最大长度。如果未设置或所设置的值小于1，Java将使用默认值50。
                     */
                    .option(ChannelOption.SO_BACKLOG, 128)

                    /**
                     * 设置保持连接状态。即是否启用心跳保活机制。在双方TCP套接字建立连接后（即都进入 ESTABLISHED 状态）并且在
                     * 两个小时左右，上层没有任何数据传输的情况下，这套机制才会被激活。
                     * childOption 是用来给父级 ServerChannel 之下的 Channels 设置参数的
                     */
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println(".....服务器 is ready...");

            // sync() 会同步等待连接操作结果，用户线程将在此 wait()，直到连接操作完成之后，线程被 notify(), 用户代码继续执行

            // 启动服务器（也可以在该处再绑定端口）：sync 会阻塞等待服务器启动，bind 返回 future(异步的)，但加上 sync 会同步阻塞
            ChannelFuture future = b.bind().sync();
            System.out.println(EchoServer.class.getName() + " started and listen on " + future.channel().localAddress());

            //给通道注册监听器，监控我们关心的事件
            future.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        System.out.println("监听端口 " + port + " 成功");
                    } else {
                        System.out.println("监听端口 " + port + " 失败");
                    }
                }
            });

            // 对关闭通道进行监听
            future.channel().closeFuture().sync();

            /**
             * 异步模型：
             * 异步的概念和同步相对。当一个异步过程调用发出后，调用者不能立刻得到结果。实际处理这个调用的组件在完成后，通过状态、
             * 通知和回调来通知调用者。Netty 中的 I/O 操作是异步的，包括 Bind、Write、Connect 等操作会简单的返回一个 ChannelFuture。
             * 调用者并不能立刻获得结果，而是通过 Future-Listener 机制，用户可以方便的主动获取或者通过通知机制获得 IO 操作结果。
             * Netty 的异步模型是建立在 future 和 callback 的之上的。callback 就是回调。Future 核心思想是：假设一个方法 fun，
             * 计算过程可能非常耗时，等待 fun返回显然不合适。那么可以在调用 fun 的时候，立马返回一个 Future，后续可以通过 Future
             * 去监控方法 fun 的处理过程（即：Future-Listener 机制）。
             *
             * Future 说明：表示异步的执行结果, 可以通过它提供的方法来检测执行是否完成，比如检索计算等等。ChannelFuture 是一个接口，
             * 可以添加监听器，当监听的事件发生时，就会通知到监听器。
             * 在使用 Netty 进行编程时，拦截操作和转换出入站数据只需要提供 callback 或利用 future 即可。这使得链式操作简单、高效,
             * 并有利于编写可重用的、通用的代码。Netty 框架的目标就是让你的业务逻辑从网络基础应用编码中分离出来、解脱出来。
             *
             * Future-Listener 机制：
             * 当 Future 对象刚刚创建时，处于非完成状态，调用者可以通过返回的 ChannelFuture 来获取操作执行的状态，注册监听函数来
             * 执行完成后的操作。常见有如下操作：
             * 通过 isDone 方法来判断当前操作是否完成；
             * 通过 isSuccess 方法来判断已完成的当前操作是否成功；
             * 通过 getCause 方法来获取已完成的当前操作失败的原因；
             * 通过 isCancelled 方法来判断已完成的当前操作是否被取消；
             * 通过 addListener 方法来注册监听器，当操作已完成(isDone 方法返回完成)，将会通知指定的监听器；如果 Future 对象已完成，
             * 则通知指定的监听器。
             *
             */
        } finally {

            // 释放 channel 和 块，直到线程组被关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}