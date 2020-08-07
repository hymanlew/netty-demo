package com.netty.fst.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * 其它编解码器：
 * ReplayingDecoder，它扩展了 ByteToMessageDecoder 类，使用此类就不必调用 readableBytes()方法。参数S指定了用户状态管理的类型，
 * 其中 Void 代表不需要状态管理。
 *
 * ReplayingDecoder 使用方便，但也有一些局限性：
 * 并不是所有的 ByteBuf 操作都被支持，如果调用了一个不被支持的方法，将会抛出一个 UnsupportedOperationException。
 * ReplayingDecoder 在某些情况下可能稍慢于 ByteToMessageDecoder，例如网络缓慢并且消息格式复杂时，消息会被拆成了多个碎片，速度变慢。
 *
 * LineBasedFrameDecoder：这个类在Netty内部也有使用，它使用行尾控制字符（\n或者\r\n）作为分隔符来解析数据。
 * DelimiterBasedFrameDecoder：使用自定义的特殊字符作为消息的分隔符。
 * HttpObjectDecoder：一个HTTP数据的解码器。
 * LengthFieldBasedFrameDecoder：通过指定长度来标识整包消息，这样就可以自动的处理黏包和半包消息。
 *
 *
 * 其它编码器：
 */
public class ReplayDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        System.out.println("ReplayDecoder 被调用");

        //在 ReplayingDecoder 不需要判断数据是否足够读取，内部会进行处理判断
        out.add(in.readLong());
    }
}
