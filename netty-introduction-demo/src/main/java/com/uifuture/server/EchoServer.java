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
         * Bootstrap 意思是引导，一个 Netty 应用通常由一个 Bootstrap 开始，主要作用是配置整个 Netty 程序，串联各个组件。Netty 中
         * Bootstrap 类是客户端程序的启动引导类，ServerBootstrap 是服务端启动引导类，也可以直接使用 Channel去建立服务端，但是大多
         * 数情况下无需这样做。
         *
         * EventLoopGroup 和其实现类 NioEventLoopGroup：
         * EventLoopGroup 是一组 EventLoop 的抽象，Netty 为了更好的利用多核 CPU 资源，一般会有多个 EventLoop 同时工作。NioEventLoop
         * 实际上就是工作线程，NioEventLoopGroup 是一个处理 I/O操作的多线程事件循环的线程池，线程池中的线程就是 NioEventLoop。该
         * 线程池可用于接收传入连接（且仅接收客户端连接，不做复杂的逻辑处理，为了尽可能减少资源的占用，所以取值越小越好）。EventLoopGroup
         * 提供 next 接口，可以从组里面按照一定规则获取其中一个 EventLoop来处理任务。在 Netty 服务器端编程中，一般都需要提供两个
         * EventLoopGroup：BossEventLoopGroup 和 WorkerEventLoopGroup。
         *
         * 其内部的每个 NioEventLoop绑定一个端口，即如果程序只需要监听1个端口的话，它里面只需要有一个 NioEventLoop 线程就行。通常
         * 一个服务端口即一个 ServerSocketChannel 对应一个 Selector 和一个EventLoop线程。可命名为 bossGroup。NioEventLoopGroup
         * 可命名为 workerGroup，用于处理 boss接收到的连接的流量，并将接收的连接注册进这个 worker。BossEventLoop 负责接收客户端的
         * 连接，不断轮询 Selector 将连接事件分离出来，并将 SocketChannel 交给 WorkerEventLoopGroup 来进行 IO 处理。WorkerEventLoopGroup
         * 会由 next 选择其中一个 EventLoop来将这个 SocketChannel 注册到其维护的 Selector 并对其后续的 IO 事件进行处理。
         *
         * Netty 内部都是通过线程在处理各种数据，EventLoopGroup就是用来管理调度他们的，注册Channel，管理他们的生命周期。每个 NioEventLoop
         * 都绑定了一个 Selector，所以在Netty的线程模型中，是由多个 Selecotr 在监听IO就绪事件。而 Channel 注册到 Selector，一个 Channel
         * 绑定一个 NioEventLoop，相当于一个连接绑定一个线程，这个连接所有的 ChannelHandler都是在一个线程中执行的，避免了多线程干扰。
         * 更重要的是 ChannelPipline 链表必须严格按照顺序执行的。单线程的设计能够保证 ChannelHandler 的顺序执行。
         * 一个 NioEventLoop 的 selector可以被多个 Channel注册，也就是说多个 Channel 共享一个 EventLoop。EventLoop的 Selecctor 对
         * 这些 Channel 进行检查，在监听一个端口的情况下，一个 NioEventLoop 通过一个 NioServerSocketChannel 监听端口，处理TCP连接。
         * 后端多个工作线程 NioEventLoop 处理 IO 事件。每个 Channel 绑定一个NioEventLoop线程，一个NioEventLoop线程关联一个 selector
         * 来为多个注册到它的 Channel 监听IO就绪事件。NioEventLoop 是单线程执行，保证 Channel的pipline 在单线程中执行，保证了 ChannelHandler
         * 的执行顺序。
         *
         * NioEventLoopGroup 是一个处理I/O操作的多线程事件循环。创建的两个线程组 bossGroup 和 workerGroup，两个都是无限循环。
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

                    // 该方法用来设置一个服务器端的通道实现，产生一个Channel用来接收连接。NioServerSocketChannel 对应 TCP, NioDatagramChannel 对应 UDP
                    .channel(NioServerSocketChannel.class)

                    //设置 socket 地址使用所选的端口
                    .localAddress(new InetSocketAddress(port))

                    // 该 handler对应 bossGroup, childHandler 对应 workerGroup
                    //.handler(null)

                    // 创建一个通道初始化对象(匿名对象)，ChannelInitializer 配置一个新的 SocketChannel，用于向你的 Channel 中添加 ChannelInboundHandler 的实现
                    // 该方法用于设置业务处理类（自定义的 handler），即给 workerGroup 的 EventLoop 对应的 pipeline 管道设置处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) {

                            // 可以使用一个集合来统一管理 SocketChannel，在推送消息时，将业务加入到各个channel 对应的 NIOEventLoop 的 taskQueue 或者 scheduleTaskQueue 中即可
                            System.out.println("客户 socketchannel hashcode=" + ch.hashCode());

                            /**
                             * Netty 中通过 ChannelPipeline 来保证 ChannelHandler 之间的处理顺序。每一个 Channel 对象创建的时候，都会自动创
                             * 建一个关联的 ChannelPipeline对象（通过 pipeline() 方法获取这个对象实例）。因为 ChannelPipleLine 的创建是定义
                             * 在AbstractChannel的构造方法中的，而每个 Channel只会被创建一次，只会调用一次构造方法，因此每个 Channel 实例只
                             * 对应唯一一个 ChannelPipleLine 实例。其具体创建过程实际上是通过 return new DefaultChannelPipeline(this); 实现的。
                             * DefaultChannelPipeline 是 ChannelPipeline的默认实现类。
                             * DefaultChannelPipeline 内部是通过一个双向链表记录 ChannelHandler的先后关系，而双向链表的节点是 AbstractChannelHandlerContext 类。
                             * DefaultChannelPipeline 内部通过两个哨兵节点 HeadContext 和 TailContext 作为链表的开始和结束，设置哨兵可以在
                             * 移除节点的时候，不需要判断是否是最后一个节点。
                             *
                             * 1、默认情况下，一个ChannelPipeline实例中，同一类型 ChannelHandler只能被添加一次，如果添加多次，则会抛出异常。
                             * 如果需要多次添加同一类型的 ChannelHandler的话，则需要在该 ChannelHandler实现类上添加 @Sharable注解。
                             *
                             * 2、在 ChannelPipeline中，每一个 ChannelHandler都有一个名字，而且名字必须是唯一的，如果名字重复了，则会抛出异常。
                             * 如果添加 ChannelHanler的时候没有显示的指定名字，则会按照规则其起一个默认的名字。
                             *
                             * 3、ChannelHanler 默认命名规则如下，如果 ChannelPipeline 中某种类型的 handler 实例只有一个，如XHandler，YHandler，
                             * 则其名字分别为 XHandler#0，YHandler#0。如果同一类型的 Handler有多个实例，则每次之后的编号加1。
                             *
                             * ChannelPipeline 是一个 Handler 的集合，它负责处理和拦截 inbound 或者 outbound 的事件和操作，相当于
                             * 一个贯穿 Netty 的链。也可以理解为 ChannelPipeline 是保存 ChannelHandler 的 List，用于处理或拦截 Channel 的入
                             * 站事件和出站操作。它实现了一种高级形式的拦截过滤器模式，使用户可以完全控制事件的处理方式，以及 Channel 中各个的
                             * ChannelHandler 如何相互交互。在 Netty 中每个 Channel 都有且仅有一个 ChannelPipeline 与之对应，它们的组成关系
                             * 为：一个 Channel 包含了一个 ChannelPipeline，而 ChannelPipeline 中又维护了一个由 ChannelHandlerContext 组成
                             * 的双向链表，并且每个 ChannelHandlerContext 中又关联着一个 ChannelHandler。入站事件和出站事件在一个双向链表中，
                             * 入站事件会从链表 head 往后传递到最后一个入站的 handler，出站事件会从链表 tail 往前传递到最前一个出站的 handler，
                             * 两种类型的 handler 互不干扰。
                             *
                             * 得到管道
                             */
                            ChannelPipeline pipeline = ch.pipeline();

                            // ChannelPipeline 用于存放管理 ChannelHandler，ChannelHandler 用于处理请求响应的业务逻辑相关代码
                            // 配置通信数据的处理逻辑，可以 addLast 多个
                            pipeline.addLast(new EchoServerHandler());

                            // 也可以加入一个 netty 提供的 httpServerCodec，即处理 http 的编解码器（codec = coder + decoder）
                            pipeline.addLast("MyHttpServerCodec",new HttpServerCodec());
                            // 增加一个自定义的 Httphandler 到链中的最后一个位置，用于接收浏览器发送的 HTTP 请求。
                            pipeline.addLast("MyHttpServerHandler", new HttpServerHandler());
                        }
                    })

                    /**
                     * 该方法用于给 ServerChannel 添加配置参数，Netty 在创建 Channel 实例后，一般都需要设置 ChannelOption 参数。
                     *
                     * SO_BACKLOG：
                     * 当服务器请求处理线程全满时，用来接收已完成三次握手的请求的队列的最大长度。即 TCP缓冲区。它对应了 TCP/IP 协议 listen 函
                     * 数中的 backlog 参数，用来初始化服务器可连接队列的大小。
                     * 服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接。多个客户端来的时候，服务端将不能处理的客户端连
                     * 接请求放在队列中等待处理，backlog 参数指定了队列的大小。
                     *
                     * 服务器的 TCP 内核维护两个队列 A 和 B：
                     * 客户端向服务端请求 connect 时，发送SYN（第一次握手），服务端收到 SYN 后，向客户端发送 SYN ACK（第二次握手），TCP内核将
                     * 连接放入队列 A。客户端收到后向服务端发送 ACK（第三次握手），TCP内核将连接从 A 转到 B，accept 返回，连接完成。A/B 队列的
                     * 长度和即为 BACKLOG，当 accept 速度跟不上时（也就是同时握手过多），A/B 队列使得 BACKLOG 满了，客户端连接就会被 TCP 内核
                     * 拒绝。此时可以调大 SO_BACKLOG 来缓解这一现象，默认值为 50。如果未设置或所设置的值小于1，也是使用默认值。
                     */
                    .option(ChannelOption.SO_BACKLOG, 128)

                    /**
                     * SO_KEEPALIVE：
                     * 设置保持连接状态。即是否启用心跳保活机制。在双方TCP套接字建立连接后（即都进入 ESTABLISHED 状态）并且在两个小时左右，上层
                     * 没有任何数据传输的情况下，这套机制才会被激活。
                     * childOption 是用来给接收到的通道添加配置，即给父级 ServerChannel 之下的 Channels 设置参数。
                     */
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println(".....服务器 is ready...");

            /**
             * Future、ChannelFuture 接口：
             * Netty 中所有的 IO 操作都是异步的，因为一个操作可能不会立即返回，不能立刻得知消息是否被正确处理。但是可以过一会等它执行完成或者直
             * 接注册一个监听，具体的实现就是通过 Future 和 ChannelFutures，他们可以注册一个监听，当操作执行成功或失败时监听会自动触发注册的监
             * 听事件。使用其 addListener() 方法注册一个 ChannelFutureListener，以便在某个操作完成时（无论是否成功）得到通知。ChannelFuture
             * 表示 Channel 中异步I/O操作的结果状态。
             * 此外不建议在 ChannelHandler 中调用 await()，因为 ChannelHandler 中事件驱动的方法是被一个 I/O 线程调用，可能一直不会完成，那么
             * await() 也可能被I/O线程调用，同样会一直 block，因此会产生死锁。所以要使用 addListener(new ChannelFutureListener() {}) 的方法。
             *
             * Channel channel()，返回当前正在进行 IO 操作的通道。
             * ChannelFuture sync()，等待异步操作执行完毕，用户线程将在此 wait()，直到操作完成后，线程被 notify()，用户代码才继续执行。
             * closeFuture()，当 Channel 关闭时返回一个 ChannelFuture，用于链路检测。
             */
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

            // 释放 channel 和 块，断开连接，关闭线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}