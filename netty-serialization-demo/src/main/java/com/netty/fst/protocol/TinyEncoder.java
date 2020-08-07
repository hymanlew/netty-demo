package com.netty.fst.protocol;

import com.netty.fst.codec.FstSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty 编解码器：
 * 当 Netty 发送或者接受一个消息的时候，就将会发生一次数据转换。入站消息会被解码：从字节转换为另一种格式（比如java对象）；如果是
 * 出站消息，它会被编码成字节。Netty 提供一系列实用的编解码器，他们都实现了ChannelInboundHadnler或者ChannelOutboundHandler接口。
 *
 * Netty 的 handler 链的调用机制：
 * 不论是解码器 handler 还是编码器 handler，接收的消息类型必须与待处理的消息类型一致，否则该 handler 不会被执行。在解码器进行数据
 * 解码时，需要判断缓存区(ByteBuf)的数据是否足够 ，否则接收到的结果可能会与期望结果不一致。
 *
 * 对于客户端来说：
 * 出站（Channelpipeline -- ClientHandler -- Encoder -- Outboundhandler -- socket）
 * 入站（socket -- Inboundhandler -- Decoder -- ClientHandler -- Channelpipeline）
 *
 * 对于服务端来说：
 * 入站（socket -- Inboundhandler -- Decoder -- ServerHandler -- Channelpipeline）
 * 出站（Channelpipeline -- ServerHandler -- Encoder -- Outboundhandler -- socket）
 */
@ChannelHandler.Sharable
public class TinyEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    public TinyEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 编码
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) {

        if (genericClass.isInstance(in)) {
            byte[] data = FstSerializer.serialize(in);
            out.writeBytes(data);
        }

        //System.out.println("MyLongToByteEncoder encode 被调用");
        //System.out.println("msg=" + "long 值" + 1L);
        //out.writeLong(1L);
    }

}
