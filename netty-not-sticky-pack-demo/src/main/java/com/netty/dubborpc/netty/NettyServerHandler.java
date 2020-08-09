package com.netty.dubborpc.netty;

import com.netty.dubborpc.customer.ClientBootstrap;
import com.netty.dubborpc.provider.HelloServiceImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 获取客户端发送的消息，并调用服务
        System.out.println("msg=" + msg);

        // 客户端在调用服务器的 api 时，需要定义一个协议。比如要求每次发消息是都必须以某个字符串开头 "#你好"
        if(msg.toString().startsWith(ClientBootstrap.PROTOCOL)) {

            String result = new HelloServiceImpl().hello(msg.toString().substring(msg.toString().lastIndexOf("#") + 1));
            ctx.writeAndFlush(result);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
