package com.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 通道（Channel）：
 * 用于源节点与目标节点的连接。在 Java NIO中负责缓冲区中数据的传输。Channel 本身不存储数据，因此需要配合缓冲区进行传输。
 * 通道的主要实现类，java.nio.channels.Channel 接口：
 *
 * FileChannel，从文件中读写数据，本地。
 * DatagramChannel，通过UDP读写网络中的数据。
 * SocketChannel，能通过TCP读写网络中的数据。
 * SocketChannel是一个连接到TCP网络套接字的通道。
 * ServerSocketChannel，监听新进来的TCP连接，像标准IO中 的ServerSocket一样。但 ServerSocketChannel 会对每一个新进来的连接
 * 都会创建一个SocketChannel。
 *
 * 除了 FileChannel，其他的网络通道主要用于非阻塞 IO。并且要注意 Filechannel 不能切换成非阻塞 IO。
 *
 * 获取通道的方式，Java 针对支持通道的类提供了 getChannel() 方法：
 * 本地 IO：FileInputStream/FileOutputStream，RandomAccessFile。
 * 网络IO：Socket，ServerSocket，DatagramSocket。
 *
 * 在 JDK 1.7 中的 NIO.2 针对各个通道提供了静态方法 open()，为其 Files 工具类提供了 newByteChannel()。
 * Java NIO的通道类似流，但又有些不同：
 * 它既可以从通道中读取数据，又可以写数据到通道。但流的读写通常是单向的。通道可以异步地读写。
 * 通道中的数据总是要先读到一个Buffer，或者总是要从一个Buffer中写入。即从通道读取数据到缓冲区，或从缓冲区写入数据到通道。
 */
public class TestNonBlockingNIO {
	
	//客户端
	public void client() throws IOException{
		// 获取通道
		SocketChannel sChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9898));

		// 切换非阻塞模式
		sChannel.configureBlocking(false);

		// 分配指定大小的缓冲区
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		// 发送数据给服务端
		Scanner scan = new Scanner(System.in);
		
		while(scan.hasNext()){
			String str = scan.next();
			buf.put((new Date().toString() + "\n" + str).getBytes());
			buf.flip();
			sChannel.write(buf);
			buf.clear();
		}
		// 关闭通道
		sChannel.close();
	}

	//服务端
	public void server() throws IOException{
		// 获取通道
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		
		// 切换非阻塞模式
		ssChannel.configureBlocking(false);
		
		// 绑定连接
		ssChannel.bind(new InetSocketAddress(9898));
		
		// 获取选择器
		Selector selector = Selector.open();
		
		// 将通道注册到选择器上, 并且指定“监听接收事件”
		ssChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		// 轮询式的获取选择器上已经“准备就绪”的事件
		while(selector.select() > 0){
			
			// 获取当前选择器中所有注册的“选择键(已就绪的监听事件)”
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			
			while(it.hasNext()){
				// 获取准备“就绪”的是事件
				SelectionKey sk = it.next();
				
				// 判断具体是什么事件准备就绪
				if(sk.isAcceptable()){
					// 若“接收就绪”，获取客户端连接
					SocketChannel sChannel = ssChannel.accept();
					
					// 切换非阻塞模式
					sChannel.configureBlocking(false);
					
					// 将该通道注册到选择器上
					sChannel.register(selector, SelectionKey.OP_READ);
				}else if(sk.isReadable()){
					// 获取当前选择器上“读就绪”状态的通道
					SocketChannel sChannel = (SocketChannel) sk.channel();
					
					// 读取数据
					ByteBuffer buf = ByteBuffer.allocate(1024);
					
					int len = 0;
					while((len = sChannel.read(buf)) > 0 ){
						buf.flip();
						System.out.println(new String(buf.array(), 0, len));
						buf.clear();
					}
				}
				
				// 取消选择键 SelectionKey，否则会一定被循环使用。
				it.remove();
			}
		}
	}

	public void send() throws IOException{
		DatagramChannel dc = DatagramChannel.open();

		dc.configureBlocking(false);

		ByteBuffer buf = ByteBuffer.allocate(1024);

		Scanner scan = new Scanner(System.in);

		while(scan.hasNext()){
			String str = scan.next();
			buf.put((new Date().toString() + ":\n" + str).getBytes());
			buf.flip();
			dc.send(buf, new InetSocketAddress("127.0.0.1", 9898));
			buf.clear();
		}

		dc.close();
	}

	public void receive() throws IOException{
		DatagramChannel dc = DatagramChannel.open();

		dc.configureBlocking(false);

		dc.bind(new InetSocketAddress(9898));

		Selector selector = Selector.open();

		dc.register(selector, SelectionKey.OP_READ);

		while(selector.select() > 0){
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();

			while(it.hasNext()){
				SelectionKey sk = it.next();

				if(sk.isReadable()){
					ByteBuffer buf = ByteBuffer.allocate(1024);

					dc.receive(buf);
					buf.flip();
					System.out.println(new String(buf.array(), 0, buf.limit()));
					buf.clear();
				}
			}

			it.remove();
		}
	}

}
