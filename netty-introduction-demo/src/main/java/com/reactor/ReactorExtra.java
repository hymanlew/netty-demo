package com.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Reactor 模式：
 * 传统阻塞 I/O 服务模型的问题无非就是服务器端的线程无限制的增长而导致服务器崩掉，那我们就对征下药，有两种解决方案：
 *
 * 1，基于 I/O 复用模型：多个连接共用一个阻塞对象，应用程序只需要在一个阻塞对象等待，无需阻塞等待所有连接。当某个连接有新的数据
 * 可以处理时，操作系统通知应用程序，线程从阻塞状态返回，开始进行业务处理。Reactor 对应的叫法：反应器模式，分发者模式(Dispatcher) ，
 * 通知者模式(notifier)。
 *
 * 2，基于线程池复用线程资源：不必再为每个连接创建线程，限制线程的生成，又可以复用空闲的线程。将连接完成后的业务处理任务分配给线
 * 程进行处理，一个线程可以处理多个连接的业务。并且在 jdk1.5也是这样做的，即 new ThreadPoolExecutor(...)，并调用其 execute(new
 * taskThread(socket)) 方法。但这只是解决了服务器端不会因为并发太多而死掉，而没有解决因为并发大而响应越来越慢的问题。即这只是用
 * 了一个假的 nio。
 *
 * I/O 复用结合线程池，就是 Reactor 模式基本设计思想：
 * Reactor 模式，就是通过一个或多个输入同时传递给服务处理器的模式（基于事件驱动）。服务器端程序处理传入的多个请求，并将它们同步
 * 分派到相应的处理线程，因此 Reactor 模式也叫 Dispatcher模式。Reactor 模式使用 IO 复用监听事件，收到事件后，分发给某个线程（
 * 进程），这一点就是网络服务器高并发处理的关键。
 *
 *
 * Reactor 模式中核心组成：
 * Reactor：它在一个单独的线程中运行，负责监听和分发事件，分发给适当的处理程序来对 IO 事件做出反应。它就像公司的电话接线员，接听来自客户的电话并将线路转移到适当的联系人。
 * Handlers：处理程序执行 I/O 事件要完成的实际事件，类似于客户想要与之交谈的公司中的实际官员。Reactor 通过调度适当的处理程序来响应 I/O 事件，处理程序执行非阻塞操作。
 *
 * 1，单 Reactor 单线程（群聊聊天室项目），方案说明：
 * Select 是前面 I/O 复用模型介绍的标准网络编程 API，可以实现应用程序通过一个阻塞对象监听多路连接请求，Reactor 对象通过 Select
 * 监控客户端请求事件，收到事件后通过 Dispatch 进行分发。如果是建立连接请求事件，则由 Acceptor 通过 Accept 处理连接请求，然后创
 * 建一个 Handler 对象处理连接完成后的后续业务处理。如果不是建立连接事件，则 Reactor 会分发调用连接对应的 Handler 来响应。Handler
 * 会完成 Read → 业务处理 → Send 的完整业务流程。
 * 结合实例：服务器端用一个线程通过多路复用搞定所有的 IO 操作（包括连接，读、写等），编码简单，清晰明了，但是如果客户端连接数量较
 * 多，将无法支撑，前面的 NIO 案例就属于这种模型。
 *
 * 方案优缺点分析：
 * 优点：模型简单，没有多线程、进程通信、竞争的问题，全部都在一个线程中完成。
 * 缺点：性能问题，只有一个线程，无法完全发挥多核 CPU 的性能。Handler 在处理某个连接上的业务时，整个进程无法处理其他连接事件，很
 * 容易导致性能瓶颈。可靠性问题，线程意外终止，或者进入死循环，会导致整个系统通信模块不可用，不能接收和处理外部消息，造成节点故障。
 * 使用场景：客户端的数量有限，业务处理非常快速，比如 Redis在业务处理的时间复杂度 O(1) 的情况。
 *
 *
 * 2，单 Reactor 多线程，方案说明：
 * Reactor 对象通过 select 监控客户端请求事件，收到事件后，通过 dispatch 进行分发。如果建立连接请求，则由 Acceptor 接收器通过
 * accept 处理连接请求，然后创建一个 Handler 对象处理完成连接后的各种事件。如果不是连接请求，则由 reactor 分发调用连接对应的
 * handler 来处理。handler 只负责响应事件，不做具体的业务处理，通过 read 读取数据后，会分发给后面的 worker 线程池的某个线程处
 * 理业务。worker 线程池会分配独立线程完成真正的业务，并将结果返回给 handler，handler收到响应后，通过 send 将结果返回给 client。
 *
 * 方案优缺点分析：
 * 优点：可以充分的利用多核 cpu 的处理能力。
 * 缺点：多线程数据共享和访问比较复杂，并且 reactor 处理所有的事件的监听和响应，是在单线程中运行的，在高并发场景容易出现性能瓶颈。
 *
 *
 * 3，主从 Reactor 多线程，方案说明：
 * 针对单 Reactor 多线程模型中，Reactor 是在单线程中运行，高并发场景下容易成为性能瓶颈，可以让 Reactor 在多线程中运行。
 * Reactor 主线程 MainReactor 对象通过 select 监听连接事件，收到事件后，通过 Acceptor 处理连接事件。当 Acceptor 处理连接事件
 * 后，MainReactor 将连接分配给 SubReactor，subreactor 将连接加入到连接队列进行监听，并创建 handler 进行各种事件处理。当有新事
 * 件发生时，subreactor 就会调用对应的 handler 处理。handler 通过 read 读取数据，分发给后面的 worker 线程处理。worker 线程池
 * 分配独立的 worker 线程进行业务处理，并返回结果。handler 收到响应的结果后，再通过send 将结果返回给 client。Reactor 主线程可以
 * 对应多个 Reactor 子线程，即 MainRecator 可以关联多个 SubReactor。
 *
 * 方案优缺点说明：
 * 优点：父线程与子线程的数据交互简单职责明确，父线程只需要接收新连接，子线程完成后续的业务处理。父线程与子线程的数据交互简单，
 * Reactor 主线程只需要把新连接传给子线程，子线程无需返回数据。
 * 缺点：编程复杂度较高。
 * 结合实例：这种模型在许多项目中广泛使用，包括 Nginx 主从 Reactor 多进程模型，Memcached 主从多线程，Netty 主从多线程模型的支持。
 *
 *
 * 3 种模式用生活案例来理解：
 * 单 Reactor 单线程，前台接待员和服务员是同一个人，全程为顾客服务。
 * 单 Reactor 多线程，1 个前台接待员，多个服务员，接待员只负责接待，服务员负责为顾客服务。
 * 主从 Reactor 多线程，多个前台接待员，多个服务生。
 *
 *
 * Reactor 模式具有如下的优点：
 * 响应快，不必为单个同步时间所阻塞，虽然 Reactor 本身依然是同步的，但可以最大程度的避免复杂的多线程及同步问题，并且避免了多线程
 * /进程的切换开销。
 * 扩展性好，可以方便的通过增加 Reactor 实例个数来充分利用 CPU 资源。
 * 复用性好，Reactor 模型本身与具体事件处理逻辑无关，具有很高的复用性。
 *
 * Reactor 编程的缺点：
 * 相比传统的简单模型，Reactor 增加了一定的复杂性，因而有一定的门槛，并且不易于调试。
 * Reactor 模式需要底层的 Synchronous Event Demultiplexer 支持，比如 Java 中的 Selector 支持，操作系统的 select 系统调用支持，
 * 如果要自己实现 Synchronous Event Demultiplexer 可能不会有那么高效。
 * Reactor 模式在 IO 读写数据时还是在同一个线程中实现的，即使使用多个 Reactor 机制，那些共享一个 Reactor 的 Channel 如果出现一
 * 个长时间的数据读写，也会影响这个Reactor中其他Channel的相应时间。比如在大文件传输时，IO操作就会影响其他Client的相应时间。因而
 * 对这种操作，使用传统的 Thread-Per-Connection 或许是一个更好的选择，或使用改进版的 Reactor 模式如 Proactor 模式。
 */
public class ReactorExtra {

    // Reactor持续改进，对于多个CPU的机器，为充分利用系统资源，将Reactor拆分为两部分。代码如下：
    class MthreadReactor implements Runnable {

        // subReactors集合, 一个 selector 代表一个 subReactor
        Selector[] selectors = new Selector[2];
        int next = 0;
        final ServerSocketChannel serverSocket;

        // Reactor初始化
        public MthreadReactor(int port) throws IOException {

            selectors[0] = Selector.open();
            selectors[1] = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(port));

            //非阻塞
            serverSocket.configureBlocking(false);

            // 分步处理，第一步，接收 accept 事件
            SelectionKey sk = serverSocket.register(selectors[0], SelectionKey.OP_ACCEPT);
            //attach callback object, Acceptor
            sk.attach(new Acceptor());
        }

        @Override
        public void run() {
            try {

                while (!Thread.interrupted()) {
                    for (int i = 0; i < 2; i++) {

                        selectors[i].select();
                        Set selected = selectors[i].selectedKeys();
                        Iterator it = selected.iterator();

                        while (it.hasNext()) {

                            // Reactor 负责收到事件后调用 handler 处理
                            SelectionKey k = (SelectionKey) it.next();
                            Runnable r = (Runnable) (k.attachment());

                            // 调用之前注册的 callback 对象
                            if (r != null) {
                                r.run();
                            }
                        }
                        selected.clear();
                    }
                }
            } catch (IOException ex) {
            }
        }


        class Acceptor {
            public synchronized void run() throws IOException {

                // 主 selector 负责 accept
                SocketChannel connection = serverSocket.accept();
                if (connection != null) {

                    // 选个 subReactor 去负责处理接收到的 connection
                    new ReactorInstru.Handler(selectors[next], connection);
                }
                if (++next == selectors.length) {
                    next = 0;
                }
            }
        }
    }
}
