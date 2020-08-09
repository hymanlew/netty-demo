package com.netty.dubborpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    /**
     * 上下文
     */
    private ChannelHandlerContext context;

    /**
     * 返回的结果
     */
    private String result;

    /**
     * 客户端调用方法时，传入的参数
     */
    private String para;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(" channelActive 被调用  ");
        context = ctx;
    }

    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(" channelRead 被调用  ");
        result = msg.toString();
        notify();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    /**
     * 被代理对象调用，发送数据给服务器 -> wait -> 等待被唤醒(channelRead) -> 返回结果。
     * 因此 call() 与 channelRead() 是相互同步的方法，所以要加上锁，并使用 wait notify 唤醒。
     *
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {
        System.out.println(" call1 被调用  ");
        context.writeAndFlush(para);

        //等待channelRead 方法获取到服务器的结果后，唤醒
        wait();
        System.out.println(" call2 被调用  ");
        return  result;
    }

    void setPara(String para) {
        System.out.println(" setPara  ");
        this.para = para;
    }
}
