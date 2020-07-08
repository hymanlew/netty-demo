package com.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 使用 NIO 完成网络通信的三个核心：
 * 
 * 1. 通道（Channel）：负责连接
 * 	   java.nio.channels.Channel 接口：
 * 			|--SelectableChannel
 * 				|--SocketChannel
 * 				|--ServerSocketChannel
 * 				|--DatagramChannel
 * 
 * 				|--Pipe.SinkChannel
 * 				|--Pipe.SourceChannel
 * 
 * 2. 缓冲区（Buffer）：负责数据的存取
 * 
 * 3. 选择器（Selector）：是 SelectableChannel 的多路复用器。用于监控 SelectableChannel 的 IO 状况
 *
 * NIO有同步阻塞和同步非阻塞两种模式。
 * 一般讲的是同步非阻塞，服务器实现模式为一个请求一个线程，但客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接
 * 有I/O请求时才启动一个线程进行处理。
 * 服务器实现模式为一个请求一个线程，即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接有I/O请求时才启动一个线
 * 程进行处理。用户进程也需要时不时的询问IO操作是否就绪，这就要求用户进程不停的去询问。 其中目前JAVA的 NIO 就属于同步非阻塞IO。
 */
public class TestBlockingNIO {

	//客户端
	public void client() throws IOException{
		// 获取通道
		SocketChannel sChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9898));

		/**
		 * 从NIO.2 开始，文件的创建，读取和写入操作都支持一个可选参数 OpenOption，它用来配置外面如何打开或是创建一个文件。
		 * 实际上 OpenOption 是 java.nio.file 包中一个接口，它有两个实现类：LinkOption 和 StandardOpenOption。都是选项枚举。
		 *
		 * StandardOpenOption:
		 * READ			以读取方式打开文件
		 * WRITE　　		已写入方式打开文件
		 * CREATE		如果文件不存在，创建
		 * CREATE_NEW	如果文件不存在，创建；若存在，异常。
		 * APPEND		在文件的尾部追加
		 * SPARSE		文件不够时创建新的文件
		 * SYNC			同步文件的内容和元数据信息随着底层存储设备
		 * DSYNC		同步文件的内容随着底层存储设备 
		 * DELETE_ON_CLOSE		当流关闭的时候删除文件
		 * TRUNCATE_EXISTING	把文件设置为0字节
		 *  
		 * LinkOption :
		 * NOFOLLOW_LINKS	不包含符号链接的文件
		 */
		FileChannel inChannel = FileChannel.open(Paths.get("1.jpg"), StandardOpenOption.READ);
		
		// 分配指定大小的缓冲区
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		// 读取本地文件，并发送到服务端
		while(inChannel.read(buf) != -1){
			buf.flip();
			sChannel.write(buf);
			buf.clear();
		}

		// 关闭输出通道，否则无法读取
		sChannel.shutdownOutput();

		//接收服务端的反馈
		int len = 0;
		while((len = sChannel.read(buf)) != -1){
			buf.flip();
			System.out.println(new String(buf.array(), 0, len));
			buf.clear();
		}

		// 关闭通道
		inChannel.close();
		sChannel.close();
	}
	
	//服务端
	public void server() throws IOException{
		// 获取通道
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		FileChannel outChannel = FileChannel.open(Paths.get("2.jpg"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		
		// 绑定连接
		ssChannel.bind(new InetSocketAddress(9898));
		// 获取客户端连接的通道
		SocketChannel sChannel = ssChannel.accept();
		// 分配指定大小的缓冲区
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		// 接收客户端的数据，并保存到本地
		while(sChannel.read(buf) != -1){
			buf.flip();
			outChannel.write(buf);
			buf.clear();
		}

		// 发送反馈给客户端
		buf.put("服务端接收数据成功".getBytes());
		buf.flip();
		sChannel.write(buf);

		// 关闭通道
		sChannel.close();
		outChannel.close();
		ssChannel.close();
		
	}
	
}
