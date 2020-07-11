package com.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Netty 是典型的 Reactor模型结构，关于Reactor的详尽阐释，可参照“Scalable IO in Java”中讲述的Reactor模式。地址是：
 * http://gee.cs.oswego.edu/dl/cpjslides/nio.pdf。
 * <p>
 * Reactor 模式也叫反应器模式，是大多数IO相关组件如 Netty、Redis在使用的IO模式，为什么需要这种模式，它是如何设计来解决高性能
 * 并发的呢？
 * <p>
 * 最原始的网络编程思路就是服务器用一个 while 循环，不断监听端口是否有新的套接字连接，如果有，那么就调用一个处理函数处理。
 * 这种方法的最大问题是无法并发，效率太低，如果当前的请求没有处理完，那么后面的请求只能被阻塞，服务器的吞吐量太低。之后想到了
 * 使用多线程，也就是很经典的connection per thread，每一个连接新开一个线程处理，每个线程中都独自处理同样的流程。tomcat服务器
 * 的早期版本确实是这样实现的。
 * <p>
 * 多线程并发 IO 模式，一个连接一个线程的优点是：
 * 一定程度上极大地提高了服务器的吞吐量，因为之前的请求在read阻塞以后，不会影响到后续的请求，因为他们在不同的线程中。这也是为
 * 什么“一个线程只能对应一个socket”的原因。另外有个问题，如果一个线程中对应多个 socket 连接不行吗？语法上确实可以，但是实际上
 * 没有用，每一个 socket都是阻塞的，所以在一个线程里只能处理一个socket，就算 accept 了多个也没用，前一个 socket被阻塞了，后面
 * 的是无法被执行到的。
 * <p>
 * 多线程并发 IO 模式，一个连接一个线程的缺点是：
 * 缺点在于资源要求太高，系统中创建线程是需要比较高的系统资源的，如果连接数太高，系统无法承受，而且线程的反复创建-销毁也需要代
 * 价。改进方法是：采用基于事件驱动的设计，当有事件触发时，才会调用处理器进行数据处理。使用Reactor模式，对线程的数量进行控制，
 * 一个线程处理大量的事件。
 * <p>
 * 单线程Reactor模型（Reactor模型的朴素原型）：
 * Java 的 NIO 模式的 Selector 网络通讯，其实就是一个简单的 Reactor 模型。可以说是 Reactor 模型的朴素原型。
 * 实际上 Reactor模式，是基于Java NIO的，在其基础上抽象出来两个组件，Reactor 和 Handler两个组件：
 * （1）Reactor：负责响应IO事件，当检测到一个新的事件，将其发送给相应的Handler去处理；新的事件包含连接建立就绪、读就绪、写就绪等。
 * （2）Handler：将自身（handler）与事件绑定，负责事件的处理，完成channel的读入，完成处理业务逻辑后，负责将结果写出 channel。
 * <p>
 * many client --> acceptor（可以看做是一种特殊的 handler） --> reactor --> many handler。
 * 在单线程 Reactor模型中，Reactor线程是个多面手，负责多路分离套接字，Accept新连接，并分派请求到Handler处理器中。Reactor和Hander
 * 处于一条线程执行。
 */
public class ReactorInstru {

    /**
     * 单线程模式的缺点:
     * <p>
     * 1，当其中某个 handler 阻塞时，会导致其他所有的 client的 handler都得不到执行，并且更严重的是，handler的阻塞也会导致整
     * 个服务不能接收新的 client 请求（因为 acceptor 也被阻塞了)。因为有这么多的缺陷，因此单线程 Reactor 模型用的比较少。这
     * 种单线程模型不能充分利用多核资源，所以实际使用的不多。
     * <p>
     * 2、因此，单线程模型仅仅适用于 handler 中业务处理组件能快速完成的场景。
     */

    // Reactor的代码如下：
    class Reactor implements Runnable {

        final Selector selector;
        final ServerSocketChannel serverSocket;

        // Reactor初始化
        public Reactor(int port) throws IOException {

            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(port));
            // 非阻塞
            serverSocket.configureBlocking(false);

            // 分步处理,第一步,接收accept事件
            SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            // attach callback object, Acceptor
            sk.attach(new Acceptor());
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {

                    selector.select();
                    Iterator it = selector.selectedKeys().iterator();

                    while (it.hasNext()) {
                        // Reactor 负责收到事件后调用 handler 处理
                        SelectionKey k = (SelectionKey) it.next();
                        Runnable r = (Runnable) (k.attachment());

                        // 调用之前注册的 callback 对象
                        if (r != null) {
                            r.run();
                        }
                    }
                    // 执行完后清理选择键
                    selector.selectedKeys().clear();
                }
            } catch (IOException ex) {
            }
        }


        // inner class
        class Acceptor implements Runnable {

            @Override
            public void run() {

                try {
                    SocketChannel channel = serverSocket.accept();
                    if (channel != null) {
                        new Handler(selector, channel);
                    }

                } catch (IOException ex) {
                }
            }
        }
    }


    // Handler的代码如下：
    static class Handler implements Runnable {

        final SocketChannel channel;
        final SelectionKey sk;
        ByteBuffer input = ByteBuffer.allocate(1024);
        ByteBuffer output = ByteBuffer.allocate(1024);

        static final int READING = 0, SENDING = 1;
        int state = READING;

        // Handler初始化
        public Handler(Selector selector, SocketChannel c) throws IOException {

            channel = c;
            c.configureBlocking(false);

            // Optionally try first read now，现在可以选择先读
            sk = channel.register(selector, 0);

            // 将 Handler 作为 callback 对象
            sk.attach(this);

            // 第二步，注册 Read 就绪事件
            sk.interestOps(SelectionKey.OP_READ);

            // 唤醒 selector
            selector.wakeup();

        }

        boolean inputIsComplete() {
            /* ... */
            return false;
        }

        boolean outputIsComplete() {
            /* ... */
            return false;
        }

        void process() {
            /* ... */
            return;
        }

        @Override
        public void run() {
            try {

                if (state == READING) {

                    channel.read(input);

                    if (inputIsComplete()) {
                        process();
                        state = SENDING;

                        // 第三步，如果正常执行完毕后，就设置为接收 write 就绪事件
                        sk.interestOps(SelectionKey.OP_WRITE);
                    }

                } else if (state == SENDING) {

                    channel.write(output);

                    // write 完就结束了, 关闭 select key
                    if (outputIsComplete()) {
                        sk.cancel();
                    }
                }

            } catch (IOException ex) {
            }
        }
    }


    /**
     * 多线程的 Reactor：
     * 基于线程池的改进，在线程 Reactor 模式基础上，做如下改进：
     * （1）将 Handler 处理器的执行放入线程池，多线程进行业务处理。
     * （2）对于Reactor而言，可以仍为单个线程。如果服务器为多核 CPU，为充分利用系统资源，可以将Reactor拆分为两个线程。
     * Reactor是一条独立的线程，Hander 处于线程池中执行。
     */

    // Reactor 类没有大的变化。
    // 改进的Handler的代码如下：
    class MthreadHandler implements Runnable {

        final SocketChannel channel;
        final SelectionKey selectionKey;
        ByteBuffer input = ByteBuffer.allocate(1024);
        ByteBuffer output = ByteBuffer.allocate(1024);

        static final int READING = 0, SENDING = 1;
        int state = READING;

        ExecutorService pool = Executors.newFixedThreadPool(2);
        static final int PROCESSING = 3;

        public MthreadHandler(Selector selector, SocketChannel c) throws IOException {

            channel = c;
            c.configureBlocking(false);

            // Optionally try first read now，现在可以选择先读
            selectionKey = channel.register(selector, 0);

            // 将 Handler 作为 callback 对象
            selectionKey.attach(this);

            // 第二步，注册 Read 就绪事件
            selectionKey.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }

        boolean inputIsComplete() {
            /* ... */
            return false;
        }

        boolean outputIsComplete() {
            /* ... */
            return false;
        }

        void process() {
            /* ... */
            return;
        }

        @Override
        public void run() {
            try {

                if (state == READING) {
                    read();
                } else if (state == SENDING) {
                    send();
                }

            } catch (IOException ex) { /* ... */ }
        }


        synchronized void read() throws IOException {

            channel.read(input);
            if (inputIsComplete()) {
                state = PROCESSING;
                //使用线程pool异步执行
                pool.execute(new Processer());
            }
        }

        void send() throws IOException {

            channel.write(output);
            // write 完就结束了, 关闭 select key
            if (outputIsComplete()) {
                selectionKey.cancel();
            }
        }

        class Processer implements Runnable {
            @Override
            public void run() {
                processAndHandOff();
            }
        }

        synchronized void processAndHandOff() {
            process();
            state = SENDING;
            // or rebind attachment
            // process完，开始等待 write 就绪
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

}
