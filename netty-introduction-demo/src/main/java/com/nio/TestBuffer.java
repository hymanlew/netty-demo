package com.nio;

import java.nio.ByteBuffer;

/**
 * 缓冲区（buffer）：
 * 在 java NIO 中负责数据的存取。本质上是一块可以写入数据，然后可以从中读取数据的内存。这块内存被包装成NIO Buffer对象，并提供
 * 了一组方法，用来方便的访问该块内存。 就像一个数组，可以保存多个相同类型的数据。
 *
 * Java NIO里关键的 Buffer实现（这些Buffer覆盖了你能通过IO发送的基本数据类型(boolean 除外)：byte, short, int, long, float, double 和 char）：
 * ByteBuffer
 * CharBuffer
 * DoubleBuffer
 * FloatBuffer
 * IntBuffer
 * LongBuffer
 * ShortBuffer
 * MappedByteBuffer，用于表示内存映射文件。
 * 上述缓冲区的管理方式几乎一致，都是通过 allocate() 获取缓冲区。每一个Buffer类都有一个 allocate 方法。
 *
 * 缓冲区存取数据的两个核心方法：
 * put()：存入数据到缓冲区中。
 * get()：获取缓冲区中的数据。
 *
 * 缓冲区中的四个核心属性（基本属性）：
 * 容量 (capacity) ：  表示 Buffer 最大数据容量，缓冲区容量不能为负，并且创建后不能更改（其底层就是数组数据结构）。
 * 限制 (limit)：      表示缓冲区中可以操作数据的大小。即大于 limit 后的数据不可读写。缓冲区的限制不能为负，并且不能大于其容量。
 * 位置 (position)：   表示缓冲区中正在操作数据的位置，即下一个要读取或写入的数据的索引。缓冲区的位置不能为负，并且不能大于其限制。
 * 标记 (mark)与重置 (reset)：标记是一个索引，表示记录当前 position 的位置（mark方法）。可以通过 reset 方法恢复到 mark 的位置。
 *
 * 标记、位置、限制、容量遵守以下不变式： 0 <= mark <= position <= limit <= capacity
 *
 *
 * 直接缓冲区与非直接缓冲区：
 * 非直接缓冲区：通过 allocate() 方法分配缓冲区，将缓冲区建立在 JVM 的内存中。
 * 写时，应用程序 > write > JVM用户地址空间缓存 > copy > OS内核地址空间 > write > 物理磁盘。
 * 读时，物理磁盘 > read > OS内核地址空间 > copy > JVM用户地址空间缓存 > read > 应用程序。
 *
 * 直接缓冲区：通过 allocateDirect() 方法分配直接缓冲区，将缓冲区建立在物理内存中。可以提高效率。
 * 写时，应用程序 > write > 物理内存映射文件 > write > 物理磁盘。
 * 读时，物理磁盘 > read > 物理内存映射文件 > read > 应用程序。
 *
 * 但直接缓冲区的缺点是：直接映射了物理内存，会有安全问题。并且直接操作物理内存，依靠的是 JVM 虚拟机内存，是不可控的（它需要靠
 * gc 垃圾回收机制来释放 JVM 虚拟机内存，但 gc 是不可控的），所以存在资源消耗严重的情况，性能消耗也是比较大的。
 * 并且数据写入到映射文件中后，也无法控制了，数据写入到物理内存的时间是由 OS 操作系统决定的。
 * 但如果缓冲的数据是需要长时间存储在内存中的，则可以使用这种方式。
 */
public class TestBuffer {

    public void test1(){
        // 分配一个指定大小的缓冲区，单位为字节数。每一个Buffer类都有一个 allocate 方法。
        // 0，1024，1024
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        /**
         * 使用Buffer读写数据一般遵循以下四个步骤：
         * 1，调用 put() 方法写入数据到Buffer，开启了写数据模式。
         * 2，调用 flip() 方法，开启读数据模式。
         * 3，从Buffer中读取数据。
         * 4，调用clear()方法或者compact()方法。
         *
         * 当向buffer写入数据时，buffer position 会记录下写了多少数据（写入后 position 会随之移动）。一旦要读取数据，需要通
         * 过 flip() 方法将Buffer从写模式切换到读模式。
         */
        // 5，1024，1024
        buffer.put("hyman".getBytes());
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        /**
         * 在读模式下，可以读取之前写入到buffer的所有数据。并且此时 limit 限制的值就是之前写入的数据字节大小，position 回到
         * 起始位置准备读数据，而且 capacity 容量是始终不变的。
         */
        // 0，5，1024
        buffer.flip();
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        System.out.println(new String(bytes, 0, bytes.length));
        // 5，5，1024
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        // 可重复读数据
        buffer.rewind();
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        /**
         * 一旦读完了所有的数据，就需要清空缓冲区，让它可以再次被写入。有两种方式能清空缓冲区：调用clear()或compact()方法。
         *
         * compact()方法只会清除已经读过的数据。任何未读的数据都被移到缓冲区的起始处，新写入的数据将放到缓冲区未读数据的后面。
         */
        // 0，1024，1024
        buffer.clear();
        System.out.println(buffer.position());
        System.out.println(buffer.limit());
        System.out.println(buffer.capacity());

        /**
         * clear()方法会清空整个缓冲区，但是缓冲区中的数据是依然存在的，只是处于被遗忘的状态，即不知道里面有多少数据了。因为
         * position，limit 等参数已经全部重置了。
         */
        System.out.println((char) buffer.get());
    }

    public void test2(){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put("hyman".getBytes());

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes, 0 ,2);
        System.out.println(new String(bytes, 0, 2));

        System.out.println(buffer.position());
        buffer.mark();

        buffer.get(bytes, 2, 2);
        System.out.println(new String(bytes, 2, 2));
        System.out.println(buffer.position());

        // 恢复到 mark 的位置
        buffer.reset();
        System.out.println(buffer.position());

        // 判断缓冲区中是否还有剩余数据
        if(buffer.hasRemaining()){
            // 获取缓冲区中可以操作的数据的数量
            System.out.println(buffer.remaining());
        }
    }

    public void test3(){
        // 建立直接缓冲区，没有使用内存映射文件
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        System.out.println(buffer.isDirect());

        buffer.put("hyman".getBytes());
    }
}
