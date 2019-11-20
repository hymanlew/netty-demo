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
 * Reactor编程的优点：
 * 1，响应快，不必为单个同步时间所阻塞，虽然Reactor本身依然是同步的；
 * 2，编程相对简单，可以最大程度的避免复杂的多线程及同步问题，并且避免了多线程/进程的切换开销；
 * 3，可扩展性，可以方便的通过增加Reactor实例个数来充分利用CPU资源；
 * 4，可复用性，reactor框架本身与具体事件处理逻辑无关，具有很高的复用性；
 * <p>
 * Reactor编程的缺点：
 * 1，相比传统的简单模型，Reactor增加了一定的复杂性，因而有一定的门槛，并且不易于调试。
 * 2，Reactor模式需要底层的 Synchronous Event Demultiplexer支持，比如Java中的Selector支持，操作系统的select系统调用支持，
 *    如果要自己实现Synchronous Event Demultiplexer可能不会有那么高效。
 * 3，Reactor模式在IO读写数据时还是在同一个线程中实现的，即使使用多个Reactor机制，那些共享一个Reactor的Channel如果出现一个
 *    长时间的数据读写，也会影响这个Reactor中其他Channel的相应时间，比如在大文件传输时，IO操作就会影响其他Client的相应时间，
 *    因而对这种操作，使用传统的 Thread-Per-Connection 或许是一个更好的选择，或则此时使用改进版的Reactor模式如 Proactor 模式。
 *
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
