package com.uifuture.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class ChatServer {

    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    /**
     * 初始化
     */
    public ChatServer(){

        try {
            selector = Selector.open();
            listenChannel = ServerSocketChannel.open();
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            listenChannel.configureBlocking(false);

            listenChannel.register(selector, SelectionKey.OP_ACCEPT);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 监听
     */
    public void listen(){

        try {

            while (true){
                // 返回有事件发生的通道的个数
                int count = selector.select(5000);

                // 如果有事件要处理，遍历所有已经就绪的 key
                if(count > 0){

                    // 返回一个 SelectionKey 集合，它会和该 Selector 关联
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()){
                        SelectionKey key = iterator.next();

                        // 创建连接通道
                        if(key.isAcceptable()){
                            SocketChannel socketChannel = listenChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println(socketChannel.getRemoteAddress() + "=== 上线");
                        }

                        // 读取数据
                        if(key.isReadable()){
                            readData(key);
                        }

                        // 删除当前的监听事件
                        iterator.remove();
                    }

                }else {
                    //System.out.println("等待。。。。。");
                }
            }

        }catch (Exception e){
            e.printStackTrace();

        }finally {

        }

    }

    /**
     * 服务端接收客户端消息
     * @param key
     */
    private void readData(SelectionKey key){
        SocketChannel channel = null;

        try {
            channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(2048);

            int count = channel.read(buffer);
            if(count > 0){
                String msg = new String(buffer.array());
                System.out.println("from 客户端 == " + msg);

                // 转发消息到其它通道，客户端
                sendToOtherClient(msg, channel);
            }

        }catch (Exception e){

            try {
                // 如果读取时抛出 IO 异常，则代表客户端已经断开了连接
                System.out.println(channel.getRemoteAddress() + " === 离线了");

                // 取消注册，关闭通道
                key.cancel();
                channel.close();

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 服务端转发消息到其他客户端
     */
    private void sendToOtherClient(String msg, SocketChannel self) throws Exception {
        System.out.println("服务器转发消息 ===");

        for(SelectionKey key : selector.keys()){

            // 通过 key 取出对应的所有的 SocketChannel
            Channel targetchannel = key.channel();

            // 排除自己的 channel
            if(targetchannel instanceof SocketChannel && targetchannel != self){
                SocketChannel dest = (SocketChannel)targetchannel;
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                dest.write(buffer);
            }
        }
    }


    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.listen();
    }
}
