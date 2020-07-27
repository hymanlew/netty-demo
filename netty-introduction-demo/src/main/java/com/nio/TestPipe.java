package com.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

/**
 * Java NIO 管道是2个线程之间的单向数据连接。Pipe有一个source通道和一个sink通道。数据会被写到sink通道，从source通道读取。
 * 管道，它可以关联一个或多个 handler 处理器，数据在经过管道时就可以同时进行相关的业务处理。而通道更多的是指数据的传输，读或写的操作。
 *
 * Netty 中的通道是对 java 原生网络编程 api 的封装，其顶级接口是 Channel。以 TCP 编程为例，在java中，有两种方式：
 * 1，基于BIO，JDK1.4之前，通常使用 java.net 包中的 ServerSocket 和 Socket 来代表服务端和客户端。
 * 2，基于NIO，JDK1.4 引入nio编程之后，使用 java.nio.channels 包中的 ServerSocketChannel 和 SocketChannel 来代表服务端与客户端。
 *
 * 在 Netty 中，对 java 中的 BIO、NIO 编程 api 都进行了封装，分别是：
 * 1，使用 OioServerSocketChannel，OioSocketChannel 对 java.net 包中的 ServerSocket 与 Socket 进行了封装。
 * 2，使用 NioServerSocketChannel，NioSocketChannel 对 java.nio.channels 包中的 ServerSocketChannel 和 SocketChannel 进行了封装。
 */
public class TestPipe {

	public void test1() throws IOException{
		// 获取管道
		Pipe pipe = Pipe.open();
		
		// 将缓冲区中的数据写入管道
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		Pipe.SinkChannel sinkChannel = pipe.sink();
		buf.put("通过单向管道发送数据".getBytes());
		buf.flip();
		sinkChannel.write(buf);
		
		// 读取缓冲区中的数据
		Pipe.SourceChannel sourceChannel = pipe.source();
		buf.flip();
		int len = sourceChannel.read(buf);
		System.out.println(new String(buf.array(), 0, len));
		
		sourceChannel.close();
		sinkChannel.close();
	}
	
}
