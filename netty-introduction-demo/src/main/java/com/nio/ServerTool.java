package com.nio;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class ServerTool {

    // 创建一个 FIFO 的队列，使用 addFirst()，getLast() 方法实现。
    private LinkedList<ByteBuffer> outputQueue;

    public ServerTool(){
        outputQueue = new LinkedList<>();
    }

    public LinkedList<ByteBuffer> getOutputQueue() {
        return outputQueue;
    }

    public void enQueue(ByteBuffer byteBuffer) {
        this.outputQueue.addFirst(byteBuffer);
    }

    public static void main(String[] args) {
        LinkedList list = new LinkedList();
        String s = "A,B,C,D,E";
        String[] data = s.split(",");

        for(String t : data){
            list.addFirst(t);
        }
        System.out.println(list);

        for(String t : data){
            System.out.println(list.getLast());
            list.removeLast();
        }
        System.out.println(list);
    }
}
