package com.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * ServerSocketChannel 在服务器端监听新的客户端 Socket 连接。
 * SocketChannel，网络 IO 通道，具体负责进行读写操作。NIO 把缓冲区的数据写入通道，或者把通道里的数据读到缓冲区。
 *
 * SelectionKey 的值有：
 * int OP_ACCEPT：   有新的网络连接可以 accept，值为 16
 * int OP_CONNECT：  代表连接已经建立，值为 8
 * int OP_READ：     代表读操作，值为 1
 * int OP_WRITE：    代表写操作，值为 4
 *
 * 源码中：
 * public static final int OP_READ = 1 << 0;
 * public static final int OP_WRITE = 1 << 2;
 * public static final int OP_CONNECT = 1 << 3;
 * public static final int OP_ACCEPT = 1 << 4;
 */
public class ServerNIO {

    public static void server() throws IOException {

        // 获取通道
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        // 切换非阻塞模式，使 Selector 选择器可以自由切换通道，以检查所有通道的监听状态
        ssChannel.configureBlocking(false);

        /**
         * 绑定连接：
         * ssChannel.bind()，通道直接绑定端口号，该方法默认设置挂起连接的最大数目是 0，即只支持一个连接。
         * ssChannel.socket().bind，使用 socket 绑定端口号，则默认设置挂起连接的最大数目是 50，即同时支持 50 个连接。
         */
        //ssChannel.bind(new InetSocketAddress(9898));
        ssChannel.socket().bind(new InetSocketAddress(8090));

        // 获取选择器，其底层方法就是 SelectorProvider.provider().openSelector();
        Selector selector = Selector.open();

        // 将通道注册到选择器上, 并且指定 “监听接收事件”
        ssChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 轮询式的获取选择器上已经“准备就绪”的事件
        for(;;) {

            // select() 会一直阻塞，监控所有注册的通道，直到有一个事件已经准备好为止。也可以指定等待的时间，毫秒数。
            // 该方法将 selector 对应的 SelectionKey（实时循环监听到的）加入到内部集合中并返回。
            selector.select();

            // 不阻塞，立马返还
            //selector.selectNow();

            // 获取当前选择器中所有注册的 “选择键(已就绪的监听事件)”，每个 selectionKey 都对应了一个 Channel（实现类包含 ServerSocketChannel 和 socketChannel）
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            // 并且要注意，keys 方法是返回所有注册的 SelectionKey 选择键。而 selectedKeys 方法是返回所有已经就绪等待执行的 SelectionKey 选择键。
            selector.keys();

            while(it.hasNext()){
                // 获取准备“就绪”的是事件
                SelectionKey sk = it.next();

                // 判断 sk 是可用状态的，并且具体是什么事件准备就绪
                // 若“接收就绪”，获取客户端连接
                if(sk.isValid() && sk.isAcceptable()){
                    ServerSocketChannel serverChannel = (ServerSocketChannel)sk.channel();

                    SocketChannel clientChannel = serverChannel.accept();
                    // 切换为非阻塞模式，否则会报异常
                    clientChannel.configureBlocking(false);

                    // 将该通道注册到选择器上，并附加一个标识对象，用于标识每一个通道的。每注册一个通道就会将对应的 SelectionKey 注册到 selector 中
                    clientChannel.register(selector, SelectionKey.OP_READ, new ServerTool());

                    InetAddress clientAddress = clientChannel.socket().getInetAddress();
                    System.out.println("Accepted connection form : "+clientAddress.getHostAddress());

                // 获取当前选择器上“读就绪”状态的通道
                }else if(sk.isValid() && sk.isReadable()){
                    SocketChannel sChannel = (SocketChannel) sk.channel();

                    // 读取的操作可以单独开线程，来进行异步操作
                    ServerTool tool = (ServerTool)sk.attachment();
                    int len = 0;
                    do {
                        // 读取数据
                        ByteBuffer buf = ByteBuffer.allocate(1024);
                        len = sChannel.read(buf);

                        // 如果数据已经全部读完了，即只有是空通道时（-1），或者对端 socket/outputstream 关闭时（-1），才算是结束
                        if(len == -1){

                            // 取消通道注册，关闭通道
                            sk.cancel();
                            sChannel.close();
                            break;
                        }

                        // 当当前通道中的数据读取结束之后
                        if(len == 0){

                            // 更改选择键的监听事件，增加读，写两个事件，使用通道符 | 连接。
                            sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            // 唤醒 selector。如果是开启新线程进行读操作时，则必须强迫选择器立即返回，因为已经读取结束。否则会一直阻塞
                            selector.wakeup();
                            break;
                        }

                        buf.flip();
                        System.out.println("读取客户端数据：" + new String(buf.array(), 0, len));
                        tool.enQueue(buf);
                        // 因为要存入队列中去，所以不能清空。
                        //buf.clear();

                    } while (len > 0);

                }else if(sk.isValid() && sk.isWritable()){
                    SocketChannel sChannel = (SocketChannel) sk.channel();
                    LinkedList<ByteBuffer> outputq = ((ServerTool)sk.attachment()).getOutputQueue();

                    // 写的操作可以单独开线程，来进行异步操作
                    ByteBuffer buffer = null;
                    int len = 0;
                    do {
                        if(outputq.size() == 0){
                            sk.interestOps(SelectionKey.OP_READ);
                            break;
                        }

                        buffer = outputq.getLast();
                        len = sChannel.write(buffer);

                        // 如果数据已经全部写完了，即只有是空通道时（-1），或者对端 socket/inputstream 关闭时（-1），才算是结束
                        if(len == -1){

                            // 取消通道注册，关闭通道
                            sk.cancel();
                            sChannel.close();
                            break;
                        }

                        System.out.println("写出数据：" + new String(outputq.getLast().array(), 0, len));
                        if(outputq.getLast().hasRemaining()){
                            System.out.println("写出数据：" + new String(outputq.getLast().array(), 0, len));
                        }
                        if(outputq.getLast().remaining() == 0){
                            outputq.removeLast();
                        }
                    } while (len > 0);
                }

                // 删除选择键 SelectionKey，否则会一直被循环使用。注意它只是删除了通道上的某个监听事件，而不是删除通道
                it.remove();
            }

            // 如果不是循环监听事件的话（for 函数），就需要及时关闭连接
            ssChannel.close();
        }
    }

    public static void main(String[] args) {
        try {
            server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
