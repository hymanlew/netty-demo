package com.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * Unpooled 类：它是 Netty 提供的一个专门用来操作缓冲区（即 Netty 的数据容器）的工具类。
 *
 * Netty 里的内存管理（内存分配）是通过 ByteBuf 这个类作为桥梁连接着业务代码与 jdk 底层的内存。Netty 有自己的缓存组件ByteBuf。可以使用
 * 两种方式对 ByteBuf 进行分类：按底层实现方式和按是否使用对象池。
 *
 * 按底层实现：
 * 1，HeapByteBuf，其底层实现为 JAVA堆内的字节数组。堆缓冲区与普通堆对象类似，位于JVM堆内存区可由GC回收，其申请和释放效率较高。常规 JAVA
 * 程序建议使用该缓冲区。
 *
 * 2，DirectByteBuf，其底层实现为操作系统内核空间的字节数组。直接缓冲区的字节数组位于 JVM 堆外的 NATIVE 堆，由操作系统管理申请和释放，但
 * 其引用仍由 JVM 管理。直接缓冲区由操作系统管理，一方面申请和释放效率都低于堆缓冲区，另一方面却可以大大提高IO效率。由于进行IO操作时，常
 * 规下用户空间的数据（即JAVA堆缓冲区）需要拷贝到内核空间（直接缓冲区），然后内核空间写到网络 SOCKET 或者文件中。如果在用户空间取得直接缓
 * 冲区，则可以直接向内核空间写数据，减少了一次拷贝，这就大大提高了IO效率，这也就是常说的零拷贝。并且 DirectByteBuf 不受GC的控制。
 *
 * 3，CompositeByteBuf，由以上两种方式组合实现。这也是一种零拷贝技术，即将两个缓冲区合并为一个。一般情况下需要将后一个缓冲区的数据拷贝到
 * 前一个缓冲区，而使用组合缓冲区则可以直接保存到两个缓冲区中，因为其内部实现是组合两个缓冲区，并保证用户如同操作一个普通缓冲区一样操作该
 * 组合缓冲区，从而减少拷贝操作。
 *
 * 4，Unsafe 和 非Unsafe：这里的 Unsafe 是JDK底层的对象，通过它能够直接操作到内存（直接缓冲区）。
 *
 * 按是否使用对象池：
 * UnpooledByteBuf，为了不使用对象池的缓冲区，不需要创建大量缓冲区对象时建议使用该类缓冲区。是直接通过 JDK 底层代码申请的。
 * PooledByteBuf，为对象池缓冲区，当对象释放后会归还给对象池，所以可循环使用。当需要大量且频繁创建缓冲区时，建议使用该类缓冲区。Netty4.1
 * 默认使用对象池缓冲区，4.0 默认使用非对象池缓冲区。
 *
 * 并且在 netty 的 buffer中，不需要使用 flip 进行反转。因为其底层维护了 readerindex 和 writerIndex，通过它们和 capacity 将 buffer分成
 * 三个区域：0 - readerindex 为已经读取的区域，readerindex - writerIndex 为可读的区域，writerIndex - capacity 为可写的区域。
 */
public class NettyByteBuf {

    public static void test1(){

        ByteBuf buffer = Unpooled.buffer(10);

        for(int i = 0; i < 10; i++) {
            buffer.writeByte(i);
        }
        System.out.println("capacity=" + buffer.capacity());

        for(int i = 0; i<buffer.capacity(); i++) {
            System.out.println(buffer.getByte(i));
        }

        for(int i = 0; i < buffer.capacity(); i++) {
            System.out.println(buffer.readByte());
        }
        System.out.println("执行完毕");
    }

    public static void test2(){

        // 创建ByteBuf
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello,world!", Charset.forName("utf-8"));

        // 判断是否有字节数组数据
        if(byteBuf.hasArray()) {

            byte[] content = byteBuf.array();

            //将 content 转成字符串
            System.out.println(new String(content, Charset.forName("utf-8")));
            System.out.println("byteBuf=" + byteBuf);

            // 0
            System.out.println(byteBuf.arrayOffset());
            // 0
            System.out.println(byteBuf.readerIndex());
            // 12
            System.out.println(byteBuf.writerIndex());
            // 36
            System.out.println(byteBuf.capacity());

            // 读取一个字节，然后之后 byteBuf 的可读字节数就会减 1
            //System.out.println(byteBuf.readByte());
            // 获取指定位置的字节，但该方法不会改变 byteBuf 的可读字节数。H 对应的 ASIC 码就是 104，所以输出 104
            //System.out.println(byteBuf.getByte(0));

            // 可读的字节数 12
            int len = byteBuf.readableBytes();
            System.out.println("len=" + len);

            // 使用 for 取出各个字节
            for (int i = 0; i < len; i++) {
                System.out.println((char) byteBuf.getByte(i));
            }

            // 按照某个范围读取
            System.out.println(byteBuf.getCharSequence(0, 4, Charset.forName("utf-8")));
            System.out.println(byteBuf.getCharSequence(4, 6, Charset.forName("utf-8")));
        }
    }
}
