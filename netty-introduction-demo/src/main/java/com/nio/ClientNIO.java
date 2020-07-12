package com.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientNIO implements Runnable {

    @Override
    public void run() {
        SocketChannel socketChannel;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            boolean connected = socketChannel.connect(new InetSocketAddress("localhost", 8090));

            if(!connected){
                while (!socketChannel.finishConnect()){
                    System.out.println("因为连接需要时间，客户端不会阻塞，可以做其他工作。。。");
                }
            }

            String data = "hyman--hello";
            ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
            socketChannel.write(buffer);

            Thread.sleep(2000);

            buffer = ByteBuffer.allocate(2048);
            socketChannel.read(buffer);
            byte[] bytes = new byte[buffer.limit()];
            StringBuilder builder = new StringBuilder();

            while (buffer.hasRemaining()) {
                buffer.get(bytes);
                builder.append(new String(bytes));
            }
            System.out.println("from server == " + builder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    public static void main(String[] args) {
        ClientNIO client = new ClientNIO();
        new Thread(client).start();

        // 模拟多线程
        //for(int i=0; i<3; i++){
        //    new Thread(client).start();
        //}
    }
}
