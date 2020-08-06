package com.netty.client;

import com.netty.protobuf.DataInfo;
import com.netty.protobuf.StudentPOJO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.util.Random;

public class ProtoDClientHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当通道就绪就会触发该方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        //随机的发送Student 或者 Workder 对象
        int random = new Random().nextInt(3);
        DataInfo.DMessage myMessage = null;

        //发送 DataMaster 对象
        if(0 == random) {

            myMessage = DataInfo.DMessage.newBuilder()
                    .setDataType(DataInfo.DMessage.DataType.DataMasterType)
                    .setDataMaster(DataInfo.DataMaster.newBuilder()
                            .setId(5)
                            .setName("玉麒麟 卢俊义")
                            .build())
                    .build();

        // 发送一个 Worker 对象
        } else {
            myMessage = DataInfo.DMessage.newBuilder()
                    .setDataType(DataInfo.DMessage.DataType.DataSlaveType)
                    .setDataSlave(DataInfo.DataSlave.newBuilder()
                            .setAge(20)
                            .setName("老李")
                            .build())
                    .build();
        }

        ctx.writeAndFlush(myMessage);
    }

    /**
     * 当通道有读取事件时，会触发
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

         //读取从客户端发送的DataMasterPojo.DataMaster
        DataInfo.DMessage message = (DataInfo.DMessage)msg;
        DataInfo.DMessage.DataType dataType = message.getDataType();

        if(dataType == DataInfo.DMessage.DataType.DataMasterType) {

            DataInfo.DataMaster DataMaster = message.getDataMaster();
            System.out.println("学生id=" + DataMaster.getId() + " 学生名字=" + DataMaster.getName());
        }

        ByteBuf buf = (ByteBuf) msg;
        System.out.println("服务器回复的消息:" + buf.toString(CharsetUtil.UTF_8));
        System.out.println("服务器的地址： "+ ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
