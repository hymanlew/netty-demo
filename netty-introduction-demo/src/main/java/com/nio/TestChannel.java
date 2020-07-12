package com.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 一、通道（Channel）：用于源节点与目标节点的连接。在 Java NIO 中负责缓冲区中数据的传输。Channel 本身不存储数据，因此需要配
 *     合缓冲区进行传输。
 *
 * 二、通道的主要实现类
 * 	java.nio.channels.Channel 接口：
 * 		|--FileChannel，         从文件中读写数据，本地。
 * 		|--SocketChannel，       能通过UDP读写网络中的数据。
 * 		|--ServerSocketChannel， 能通过TCP读写网络中的数据。
 * 		|--DatagramChannel，     监听新进来的TCP连接，像Web服务器那样。对每一个新进来的连接都会创建一个SocketChannel。
 *
 * 三、获取通道
 * 1. Java 针对支持通道的类提供了 getChannel() 方法
 * 		本地 IO：
 * 		FileInputStream/FileOutputStream
 * 		RandomAccessFile
 *
 * 		网络IO：
 * 		Socket
 * 		ServerSocket
 * 		DatagramSocket
 *
 * 2. 在 JDK 1.7 中的 NIO.2 针对各个通道提供了静态方法 open()
 * 3. 在 JDK 1.7 中的 NIO.2 的 Files 工具类的 newByteChannel()
 *
 * 四、通道之间的数据传输
 * transferFrom()，transferTo()
 *
 * 五、分散(Scatter) 与聚集(Gather)
 * 分散读取（Scattering Reads）：将通道中的数据分散到多个缓冲区中，按照缓冲区的顺序，依次写入到缓冲区。
 * 聚集写入（Gathering Writes）：将多个缓冲区中的数据聚集到通道中，按照缓冲区的顺序，依次写入 postion 至 limit之间的数据到通道。
 *
 * 六、字符集：Charset
 * 编码：字符串 -> 字节数组
 * 解码：字节数组  -> 字符串
 *
 * 应用程序访问请求内存的过程，及进化过程：
 * 应用程序 > 直接/非直接缓冲区 > 读写 IO 接口 > （1，2，3） > CPU。
 * 1，通过 IO 接口直接调用 CPU 的资源去处理内存。
 * 2，在 IO 接口与 CPU 之间建立 DMA 服务总线，由 DMA 向 CPU 请求权限，并全权代理 CPU 处理内存的调度。
 * 3，将 DMA 升级为 channel，通道本身就是一个完全独立的处理器（不需要向 CPU 申请权限），可以调度内存。当然它是依附于 CPU 的。
 */
public class TestChannel {

    // NIO 字符集
    public void test5() throws Exception{

        // 获取 NIO 支持的字符集类型
        Map<String, Charset> map = Charset.availableCharsets();
        Set<Map.Entry<String, Charset>> set = map.entrySet();
        for (Map.Entry<String, Charset> entry : set) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        Charset cs1 = Charset.forName("GBK");
        // 获取编码器
        CharsetEncoder ce = cs1.newEncoder();
        // 获取解码器
        CharsetDecoder cd = cs1.newDecoder();

        CharBuffer cBuf = CharBuffer.allocate(1024);
        cBuf.put("尚硅谷威武！");
        cBuf.flip();

        // 编码，转换为字节
        ByteBuffer bBuf = ce.encode(cBuf);
        // 一个中文占两个字节，所以是 12。并且输出的是字节对应的数值
        for (int i = 0; i < 12; i++) {
            System.out.println(bBuf.get());
        }

        // 解码，转换为字符串
        bBuf.flip();
        CharBuffer cBuf2 = cd.decode(bBuf);
        System.out.println(cBuf2.toString());

        // 测试不同字符集造成的乱码
        Charset cs2 = Charset.forName("UTF-8");
        bBuf.flip();
        CharBuffer cBuf3 = cs2.decode(bBuf);
        System.out.println(cBuf3.toString());
    }

    //分散和聚集
    public void test4() throws IOException{
        RandomAccessFile raf1 = new RandomAccessFile("1.txt", "rw");

        //1. 获取通道
        FileChannel channel1 = raf1.getChannel();

        //2. 分配指定大小的缓冲区
        ByteBuffer buf1 = ByteBuffer.allocate(100);
        ByteBuffer buf2 = ByteBuffer.allocate(1024);

        //3. 分散读取
        ByteBuffer[] bufs = {buf1, buf2};

        long read = 0;
        while ((read = channel1.read(bufs)) != -1){

            for (ByteBuffer byteBuffer : bufs) {
                byteBuffer.flip();
            }
            System.out.println(new String(bufs[0].array(), 0, bufs[0].limit()));
            System.out.println(new String(bufs[1].array(), 0, bufs[1].limit()));

            //4. 聚集写入
            RandomAccessFile raf2 = new RandomAccessFile("2.txt", "rw");
            FileChannel channel2 = raf2.getChannel();
            channel2.write(bufs);

            Arrays.asList(bufs).stream().forEach(buf -> buf.clear());
        }
    }

    /**
     * 通道之间的数据传输（直接缓冲区）
     *
     * @throws IOException
     */
    public void test3() throws IOException{
        FileChannel inChannel = FileChannel.open(Paths.get("d:/1.mkv"), StandardOpenOption.READ);
        FileChannel outChannel = FileChannel.open(Paths.get("d:/2.mkv"), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);

        // 要注意，在SoketChannel的实现中，SocketChannel只会传输此刻准备好的数据（可能不足 size 字节）。因此，SocketChannel
        // 可能不会将请求的所有数据（size 个字节）全部传输到 outChannel 中。
        outChannel.transferFrom(inChannel, 0, inChannel.size());

        // 并且 SocketChannel的问题在 transferTo()方法中同样存在。SocketChannel 会一直传输数据直到目标buffer被填满。
        //inChannel.transferTo(0, inChannel.size(), outChannel);

        inChannel.close();
        outChannel.close();
    }

    /**
     * 使用直接缓冲区完成文件的复制（内存映射文件）
     *
     * @throws IOException
     */
    public void test2() throws IOException{
        long start = System.currentTimeMillis();

        /**
         * 从NIO.2 开始，文件的创建，读取和写入操作都支持一个可选参数 OpenOption，用来配置外面如何打开或是创建一个文件。
         * 实际上 OpenOption 是 java.nio.file 包中一个接口，它有两个实现类：LinkOption 和 StandardOpenOption。
         *
         * StandardOpenOption:
         * READ	        以读取方式打开文件
         * WRITE　　	    以写入方式打开文件
         * CREATE	    如果文件不存在，创建
         * CREATE_NEW	如果文件不存在，创建；若存在，异常。
         * APPEND	    在文件的尾部追加
         * SPARSE	    文件不够时创建新的文件
         * SYNC	        同步文件的内容和元数据信息随着底层存储设备
         * DSYNC        同步文件的内容随着底层存储设备 
         * DELETE_ON_CLOSE	    当流关闭的时候删除文件
         * TRUNCATE_EXISTING	把文件设置为0字节
         *
         * LinkOption :
         * NOFOLLOW_LINKS	不包含符号链接的文件
         */
        FileChannel inChannel = FileChannel.open(Paths.get("d:/1.txt"), StandardOpenOption.READ);
        FileChannel outChannel = FileChannel.open(Paths.get("d:/2.txt"), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);

        /**
         * NIO 提供了 MappedByteBuffer，可以让文件直接在系统内存（即堆外的内存）中进行修改，而如何同步到文件中则由 NIO 来完成。
         * 这就不需要操作系统进行 IO 拷贝等操作。
         *
         * 它表示内存映射文件，存储到物理内存中，并指定映射方式。并且只支持 ByteBuffer。实际类型是 DirectByteBuffer。
         *
         * map() 方法参数说明：
         * 第一个是文件操作的类型，第二个是可以修改的起始位置，第三个是映射到内存的大小（单位是字节），即多少字节的文件数据被映
         * 射到内存中。但它不是指字节数组的下标位置，可操作的位置是字节大小 - 1。
         */
        MappedByteBuffer inMappedBuf = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        MappedByteBuffer outMappedBuf = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());

        // 直接对缓冲区进行数据的读写操作，直接操作缓冲区就会直接操作物理内存中的数据
        // 默认 limit 大小就等于此时 MappedByteBuffer 的大小
        byte[] dst = new byte[inMappedBuf.limit()];
        inMappedBuf.get(dst);
        outMappedBuf.put(dst);

        inChannel.close();
        outChannel.close();

        /**
         * 直接缓冲区的缺点是：直接映射了物理内存，会有安全问题。并且直接操作物理内存，依靠的是 JVM 虚拟机内存，是不可控的（
         * 它需要靠 gc 垃圾回收机制来释放 JVM 虚拟机内存，但 gc 是不可控的），所以存在资源消耗严重的情况，性能消耗也是比较大
         * 的。
         * 并且数据写入到映射文件中后，也无法控制了，数据写入到物理内存的时间是由 OS 操作系统决定的。
         * 但如果缓冲的数据是需要长时间存储在内存中的，则可以使用这种方式。
         */
        long end = System.currentTimeMillis();
        System.out.println("耗费时间为：" + (end - start));
    }

    /**
     * 利用通道完成文件的复制（非直接缓冲区）
     */
    public void test1(){
        long start = System.currentTimeMillis();

        // 获取通道。发生异常后自动关闭所有的通道及连接
        try( FileInputStream fis = new FileInputStream("d:/1.txt");
             FileOutputStream fos = new FileOutputStream("d:/2.txt");

             // 从 FileIn/OutputStream 获取对应的 FileChannel，并且其真实类型是 FileChannelImpl
             FileChannel inChannel = fis.getChannel();
             FileChannel outChannel = fos.getChannel()
        ) {

            // 分配指定大小的缓冲区
            ByteBuffer buf = ByteBuffer.allocate(1024);

            // 将通道中的数据存入缓冲区中
            while(inChannel.read(buf) != -1){

                // 切换读取数据的模式
                buf.flip();
                System.out.println(new String(buf.array()));

                // 将缓冲区中的数据写入通道中
                while (buf.hasRemaining()){
                    outChannel.write(buf);
                }

                // 清空缓冲区
                buf.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("耗费时间为：" + (end - start));
    }

}
