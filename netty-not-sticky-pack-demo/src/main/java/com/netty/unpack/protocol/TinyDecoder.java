package com.netty.unpack.protocol;

import com.netty.unpack.codec.FstSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class TinyDecoder extends ByteToMessageDecoder {

    /**
     * 头部长度字节数
     * 由于在 TinyEncoder 的 encode 方法中使用的是 writeInt，int为4个字节
     */
    private Integer HEAD_LENGTH = 4;
    private Class<?> genericClass;

    public TinyDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 解码
     *
     * @param ctx
     * @param in
     * @param out
     */
    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        //头部信息是int类型，长度是4，所以信息长度不可能小于4的
        if (in.readableBytes() < HEAD_LENGTH) {
            return;
        }
        //标记当前的 readIndex 的位置
        in.markReaderIndex();

        //读取传送过来的消息的长度。ByteBuf 的 readInt() 方法会让他的 readIndex 增加 4。指针会向前移动 4
        int dataLength = in.readInt();

        // 如果收到的消息体长度小于我们传送过来的消息长度，则 resetReaderIndex，它是配合 markReaderIndex 使用的。是把 readIndex
        // 重置到 mark 的地方。
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);
        //反序列化
        Object obj = FstSerializer.deserialize(data, genericClass);
        out.add(obj);
    }

}
