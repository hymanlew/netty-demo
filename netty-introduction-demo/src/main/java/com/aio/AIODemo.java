package com.aio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AIODemo {

    public static void main(String[] args) throws Exception{

        final AsynchronousServerSocketChannel aio = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8090));

        /**
         * accept() 本身不是异步 IO，因为它会造成阻塞。但是异步 IO 是要求立刻返回的。
         * 所以需要在 accept() 方法中传入一个回调对象（即 CompletionHandler 它是一个接口），来进行对连接的客户端进行操作。
         *
         * attachment，是要附加到 I/O 操作的对象，起到标识的作用。
         * handler，是用于对结果进行的处理的程序。
         */
        aio.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            final ByteBuffer buffer = ByteBuffer.allocate(1024);

            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                System.out.println(Thread.currentThread().getName());
                Future<Integer> writeResult = null;

                // AsynchronousSocketChannel 为异步 IO 处理后结果数据的通道。
                try {
                    buffer.clear();
                    result.read(buffer).get(100, TimeUnit.SECONDS);
                    buffer.flip();
                    writeResult = result.write(buffer);

                } catch (Exception e) {
                    e.printStackTrace();

                }finally {
                    // 出现异常之后，再重试一次
                    try {
                        aio.accept(null, this);
                        writeResult.get();
                        result.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println("failed : " + exc);
            }
        });

    }
}
