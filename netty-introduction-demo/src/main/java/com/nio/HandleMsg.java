package com.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HandleMsg implements Runnable{

    private Socket client;

    public HandleMsg(Socket client){
        this.client = client;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;

        try{
            /**
             * 验证 BIO 是否是一个连接就开启一个新线程
             */
            System.out.println("线程信息 == " + Thread.currentThread().getId());

            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new PrintWriter(client.getOutputStream());

            String input = null;
            while ((input = reader.readLine()) != null){
                System.out.println("输入 == " + input);
                writer.println(input);

                /**
                 * flush() 必须放在 while 循环内，因为客户端的 BufferedReader.readLine() 会一直阻塞等待服务器的输出。
                 * 而客户端的 BufferedReader 阻塞之后，又因为它又没关闭连接，所以也造成本 while 的 readLine() 造成阻塞。
                 * 形成死锁。
                 */
                writer.flush();

                System.out.println("阻塞 - 等待客户端发送数据 .....");
            }
            System.out.println("输入结束 ==");

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(reader != null){
                    reader.close();
                }
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
