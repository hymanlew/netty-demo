/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 处理服务器端通道
 *
 */
@ChannelHandler.Sharable
public class StringServerHandler extends SimpleChannelInboundHandler<String> {

    /**
     * 定义一个 channle 组，管理所有的 channel
     *
     * public static List<Channel> channels = new ArrayList<Channel>();
     * public static Map<String, Channel> channels = new HashMap<String,Channel>();
     *
     * GlobalEventExecutor.INSTANCE) 是全局的事件执行器，是一个单例。用于执行 ChannelGroup
     */
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * handlerAdded 方法表示连接建立时，一旦连接，则本方法第一个被执行。用于将当前channel 加入到  channelGroup
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();
        /**
         * 将该客户端加入到聊天组的信息推送给其它在线的客户端。
         * 并且 writeAndFlush 方法会将 channelGroup 中所有的 channel 埋遍历，并发送消息，所以不需要自己遍历
         */
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 加入聊天室" + sdf.format(new Date()) + " \n");
        channelGroup.add(channel);

        /**
         * 这此方法中做对客户端的注册，验证，登录等操作
         */
    }

    /**
     * 在断开连接时，将 xx 客户端离开的信息推送给当前在线的客户
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 离开了\n");
        System.out.println("channelGroup size" + channelGroup.size());
    }

    /**
     * 表示 channel 处于活动状态, 提示 xx上线
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        System.out.println(ctx.channel().remoteAddress() + " 上线了~");
    }

    /**
     * 表示 channel 处于不活动状态, 提示 xx离线了
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        System.out.println(ctx.channel().remoteAddress() + " 离线了~");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        // 遍历 channelGroup, 根据不同的情况，回送不同的消息
        Channel channel = ctx.channel();
        channelGroup.forEach(ch -> {

            // 不是当前的 channel 时才转发消息，否则回显自己发送的消息给自己
            if(channel != ch) {
                ch.writeAndFlush("[客户]" + channel.remoteAddress() + " 发送了消息" + msg + "\n");
            }else {
                ch.writeAndFlush("[自己]发送了消息" + msg + "\n");
            }
        });
    }

    /**
     * 读操作时捕获到异常时调用，一般是需要关闭通道
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        //打印异常堆栈跟踪
        cause.printStackTrace();
        //关闭通道
        ctx.close();
    }
}