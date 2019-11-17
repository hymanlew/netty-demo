package com.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {

    public static class ClientR implements Runnable {
        @Override
        public void run() {
            Socket client = null;
            PrintWriter writer = null;
            BufferedReader reader = null;

            try {
                client = new Socket();
                client.connect(new InetSocketAddress("localhost", 8090));

                writer = new PrintWriter(client.getOutputStream(), true);
                writer.println("HELLO=1");

                Thread.sleep(1000);
                writer.println("HELLO=2");

                Thread.sleep(1000);
                writer.println("HELLO=3");
                writer.flush();

                // 这里不能关闭输出流，因为 PrintWriter 会调用 Socket 的输出流，当 PrintWriter 关闭时同时也会关闭 Socket。
                //writer.close();


                /**
                 * BufferedReader.readLine() 会一直阻塞等待当前流关闭，或者对方调用 flush 刷出缓存。
                 * 并且由于服务器端不会主动关闭 Socket 连接，所以这里不能使用 while，会一直阻塞 readLine() 方法。
                 * 但如果只调用一次 readLine() 方法，就只能接收到一次服务器发送的信息。所以要使用 reader.ready() 方法进行循环。
                 */
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                //System.out.println("from server == " + reader.readLine());

                String input = null;
                StringBuilder builder = new StringBuilder();

                while (reader.ready()){
                    input = reader.readLine();
                    builder.append(input);
                }

                //do {
                //    input = reader.readLine();
                //    builder.append(input);
                //}while (reader.read() > 0);

                //while ((input = reader.readLine()) != null){
                //    builder.append(input);
                //}
                System.out.println("from server == " + builder.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    if(reader != null){
                        reader.close();
                    }
                    if(writer != null){
                        writer.close();
                    }
                    if(client != null){
                        client.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ClientR client = new ClientR();
        new Thread(client).start();

        // 模拟多线程
        //for(int i=0; i<3; i++){
        //    new Thread(client).start();
        //}
    }
}
