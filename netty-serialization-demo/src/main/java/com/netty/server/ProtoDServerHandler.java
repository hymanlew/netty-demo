package com.netty.server;

import com.netty.protobuf.DataInfo;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

//public class NettyServerHandler extends ChannelInboundHandlerAdapter {
public class ProtoDServerHandler extends SimpleChannelInboundHandler<DataInfo.DMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 生成一个 DataMaster 对象，并发送到客户端
        DataInfo.DataMaster DataMaster = DataInfo.DataMaster.newBuilder().setId(2).setName("hyman2").build();
        ctx.writeAndFlush(DataMaster);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DataInfo.DMessage msg) throws Exception {

        //根据dataType 来显示不同的信息
        DataInfo.DMessage.DataType dataType = msg.getDataType();
        if(dataType == DataInfo.DMessage.DataType.DataMasterType) {

            DataInfo.DataMaster DataMaster = msg.getDataMaster();
            System.out.println("学生id=" + DataMaster.getId() + " 学生名字=" + DataMaster.getName());

        } else if(dataType == DataInfo.DMessage.DataType.DataSlaveType) {

            DataInfo.DataSlave DataSlave = msg.getDataSlave();
            System.out.println("工人的名字=" + DataSlave.getName() + " 年龄=" + DataSlave.getAge());

        } else {
            System.out.println("传输的类型不正确");
        }
    }


    //@Override
    //public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    //
    //    // 读取从客户端发送的DataMasterPojo.DataMaster
    //    DataInfo.DMessage message = (DataInfo.DMessage)msg;
    //    DataInfo.DMessage.DataType dataType = message.getDataType();
    //
    //    if(dataType == DataInfo.DMessage.DataType.DataMasterType) {
    //
    //        DataInfo.DataMaster DataMaster = message.getDataMaster();
    //        System.out.println("学生id=" + DataMaster.getId() + " 学生名字=" + DataMaster.getName());
    //    }
    //}

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵1", CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
