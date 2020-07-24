package com.nio.zerocopy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * java 传统 IO 的服务器
 */
public class OldIO {

    class OldIOServer {

        public void main(String[] args) throws Exception {
            ServerSocket serverSocket = new ServerSocket(7001);

            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                try {
                    byte[] byteArray = new byte[4096];

                    while (true) {
                        int readCount = dataInputStream.read(byteArray, 0, byteArray.length);

                        if (-1 == readCount) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    class OldIOClient {

        public void main(String[] args) throws Exception {
            Socket socket = new Socket("localhost", 7001);

            String fileName = "protoc-3.6.1-win32.zip";
            InputStream inputStream = new FileInputStream(fileName);

            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[4096];
            long readCount;
            long total = 0;

            long startTime = System.currentTimeMillis();

            while ((readCount = inputStream.read(buffer)) >= 0) {
                total += readCount;
                dataOutputStream.write(buffer);
            }

            System.out.println("发送总字节数： " + total + ", 耗时： " + (System.currentTimeMillis() - startTime));

            dataOutputStream.close();
            socket.close();
            inputStream.close();
        }
    }
}
