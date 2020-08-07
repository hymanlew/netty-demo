package com.netty.fst.protocol;

import com.netty.fst.codec.FstSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Netty 编解码器：
 * 他们都实现了 ChannelInboundHadnler 或者 ChannelOutboundHandler 接口，并且这些类中 channelRead 方法已经被重写了。以入站为例，
 * 对于每个从入站 Channel 读取的消息，这个方法会被调用。随后，它将调用由解码器所提供的 decode() 方法进行解码，并将已经解码的字节
 * 转发给 ChannelPipeline 中的下一个 ChannelInboundHandler。
 *
 * 解码器-ByteToMessageDecoder：由于不可能知道远程节点是否会一次性发送一个完整的信息，tcp有可能出现粘包拆包的问题，这个类会对入
 * 站数据进行缓冲，直到它准备好被处理。
 */
public class TinyDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public TinyDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * decode 方法会根据接收的数据，被调用多次, 直到确定没有新的元素被添加到 list，或者是 ByteBuf 没有更多的可读字节为止。
     * 如果 list out 不为空，就会将 list 的内容传递给下一个 channelinboundhandler 处理, 该处理器的方法也会被调用多次。
     *
     * @param ctx   上下文对象
     * @param in    入站的 ByteBuf
     * @param out   List 集合，将解码后的数据传给下一个handler
     * @throws Exception
     */
    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        int size = in.readableBytes();
        byte[] data = new byte[size];
        in.readBytes(data);
        Object obj = FstSerializer.deserialize(data, genericClass);
        out.add(obj);

        //因为 long 是 8个字节, 需要判断有8个字节，才能读取一个long。int 是 4 个字节
        //if(in.readableBytes() >= 8) {
        //    out.add(in.readLong());
        //}
    }

}
