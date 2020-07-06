package com.nio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerBIO {

    public static void main(String[] args) {
        ServerSocket server = null;
        Socket client = null;

        try {
            server = new ServerSocket(8090);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * 为每一个客户端开启一个线程，如果客户端出现延时等异常，线程可能会被占用很长时间。因为数据的准备和读取都在这个线程中。
         * 此时，如果客户端数量众多，可能会消耗大量的系统资源。
         *
         * 解决方法就是，使用非阻塞的 NIO，数据准备好了再工作。
         */
        for (int i=1; i<4; i++){
            try {
                System.out.println("阻塞 - 等待客户端连接 .....");

                client = server.accept();
                System.out.println(client.getRemoteSocketAddress() + "connect!");
                new Thread(new HandleMsg(client)).start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
    }

}
