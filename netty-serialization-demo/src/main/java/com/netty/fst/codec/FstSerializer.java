/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.fst.codec;

import org.nustaq.serialization.FSTConfiguration;

/**
 * server 端主动发送消息给 client：https://blog.csdn.net/yaobo2816/article/details/49073557（已保存图片）。
 * 采用 ECHO 服务（响应式协议），也就是客户端请求什么，服务器就返回什么。在 netty 中的对象序列化，目的是为了实现对象的网络传输和
 * 本地持久化。如果使用 java 的序列化，码流较大。因此大多使用 FastjsonSerialize，KryoSerialize，FSTSerialize 等。
 *
 * http://ifeve.com/netty5-user-guide/：
 * DISCARD服务（丢弃服务，指的是会忽略所有接收的数据的一种协议）。世界上最简单的协议不是 Hello,World!，而是 DISCARD，他是一种丢
 * 弃了所有接受到的数据，并不做有任何的响应的协议。而为了实现 DISCARD 协议，唯一需要做的就是忽略所有收到的数据。
 *
 * TIME 服务（时间协议 TIME 的服务)，它与之前的协议不同的是在不接受任何请求时他会发送一个含32位的整数的消息，并且一旦消息发送就会
 * 立即关闭连接。即在操作完成时主动关闭连接。覆盖 channelActive() 方法。关闭一个Netty 应用只需要简单地通过 shutdownGracefully()
 * 方法来关闭构建的所有 NioEventLoopGroups。当 EventLoopGroup 被完全地终止，并且对应的所有 channels 都已经被关闭时，Netty会返回
 * 一个Future对象。
 */
public class FstSerializer {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    /**
     * 反序列化
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        return (T) conf.asObject(data);
    }

    /**
     * 序列化
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> byte[] serialize(T obj) {
        return conf.asByteArray(obj);
    }
}