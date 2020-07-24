package com.nio.zerocopy;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 零拷贝是网络编程的关键，很多性能优化都离不开。在 Java 程序中，常用的零拷贝有 mmap（内存映射）和 sendFile。DMA: direct memory access
 * 直接内存拷贝，不使用 CPU。
 *
 * 1，Java 传统 IO 和网络编程的一段代码：
 * File file = new File("test.txt");
 * RandomAccessFile raf = new RandomAccessFile(file, "rw");
 *
 * byte[] arr = new byte[(int) file.length()];
 * raf.read(arr);
 *
 * Socket socket = new ServerSocket(8080).accept();
 * socket.getOutputStream().write(arr);
 *
 * hard disk --> DMA 拷贝到内核缓冲 kernel buffer --> CPU 拷贝到用户缓冲 user buffer --> CPU 拷贝到 socket buffer --> DMA
 * 拷贝到协议栈 protocol engine。这样就经过了四次拷贝，三次上下文的切换。
 *
 *
 * 2，mmap（mamory map）优化：
 * mmap 通过内存映射，将文件直接映射到内核缓冲区，同时用户空间可以共享内核空间的数据。这样在进行网络传输时，就可以减少内核空间到
 * 用户控件的拷贝次数。
 *
 * hard disk --> DMA 拷贝到内核缓冲 kernel buffer --> 并且此时用户缓冲 user buffer 与内核缓冲可以共享数据 --> CPU 拷贝到 socket
 * buffer --> DMA 拷贝到协议栈 protocol engine。这样就经过了三次拷贝，三次上下文的切换。
 *
 *
 * 3，sendFile 优化：
 * Linux 2.1 版本提供了 sendFile 函数，基本原理是：数据根本不经过用户态，直接从内核缓冲区进入到 Socket Buffer，同时由于和用户
 * 态完全无关，就减少了一次上下文切换。
 *
 * hard disk --> DMA 拷贝到内核缓冲 kernel buffer --> CPU 拷贝到 socket buffer --> DMA 拷贝到协议栈 protocol engine。这样就
 * 经过了三次拷贝，两次上下文的切换。
 *
 *
 * Linux 2.4 版本中做了一些修改，避免了从内核缓冲区拷贝到 Socket buffer 的操作，直接拷贝到协议栈，从而再一次减少了数据拷贝。经过了两次拷贝，两次上下文的切换。但这里其实是有一次 cpu 拷贝，kernel buffer -> socket buffer，但拷贝的信息很少，比如内核缓冲的 lenght , 容量大小 offset
 * 等等，消耗低，可以忽略。
 *
 * 零拷贝，是从操作系统的角度来说的。因为内核缓冲区之间，没有数据是重复的（只有 kernel buffer 有一份数据）。零拷贝不仅仅带来更少的数据复制，还能带来其他的性能优势，例如更少的上下文切换，更少的 CPU 缓存伪共享以及无 CPU 校验和计算。
 * 要注意，所谓零拷贝并不是说完全没有拷贝，而是从操作系统角度，没有cpu 拷贝。DMA 拷贝是无法避免的。
 *
 *
 * mmap 和 sendFile 的区别：
 * mmap 适合小数据量读写，sendFile 适合大文件传输。
 * mmap 需要 4 次上下文切换，3 次数据拷贝；sendFile 需要 3 次上下文切换，最少 2 次数据拷贝。
 * sendFile 可以利用 DMA 方式，减少 CPU 拷贝，mmap 则不能（必须从内核拷贝到 Socket 缓冲区）。
 */
public class NewIO {

    class NewIOServer {
        public void main(String[] args) throws Exception {

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();

            InetSocketAddress address = new InetSocketAddress(7001);
            serverSocket.bind(address);

            //创建buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();

                int readcount = 0;
                while (-1 != readcount) {
                    try {
                        readcount = socketChannel.read(byteBuffer);

                    }catch (Exception ex) {
                        // ex.printStackTrace();
                        break;
                    }
                    //倒带 position = 0 mark 作废
                    byteBuffer.rewind();
                }
            }
        }
    }

    class NewIOClient {
        public void main(String[] args) throws Exception {

            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("localhost", 7001));
            String filename = "protoc-3.6.1-win32.zip";

            // 得到一个文件channel
            FileChannel fileChannel = new FileInputStream(filename).getChannel();

            // 准备发送
            long startTime = System.currentTimeMillis();

            // 在 linux 下一个 transferTo 方法就可以完成所有的数据传输。
            // 在 windows 下一次调用 transferTo 只能发送 8m 的数据, 这就需要分段传输文件, 而且要注意传输时的位置.
            // transferTo 底层使用到零拷贝
            long transferCount = fileChannel.transferTo(0, fileChannel.size(), socketChannel);

            System.out.println("发送的总的字节数 =" + transferCount + " 耗时:" + (System.currentTimeMillis() - startTime));

            //关闭
            fileChannel.close();
        }
    }
}
