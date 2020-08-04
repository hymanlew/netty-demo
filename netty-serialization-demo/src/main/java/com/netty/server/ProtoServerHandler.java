package com.netty.server;

import com.netty.protobuf.StudentPOJO;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * 1. 我们自定义一个 Handler 需要继续采用 netty 规定好的某个HandlerAdapter(规范)
 * 2. 这时我们自定义一个Handler , 才能称为一个handler
 */
//public class NettyServerHandler extends ChannelInboundHandlerAdapter {
public class ProtoServerHandler extends SimpleChannelInboundHandler<StudentPOJO.Student> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 生成一个 student 对象，并发送到客户端
        StudentPOJO.Student student = StudentPOJO.Student.newBuilder().setId(2).setName("hyman2").build();
        ctx.writeAndFlush(student);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, StudentPOJO.Student msg) throws Exception {

        // 读取从客户端发送的StudentPojo.Student
        System.out.println("客户端发送的数据 id=" + msg.getId() + " 名字=" + msg.getName());
    }


    //@Override
    //public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    //
    //    // 读取从客户端发送的StudentPojo.Student
    //    StudentPOJO.Student student = (StudentPOJO.Student) msg;
    //    System.out.println("客户端发送的数据 id=" + student.getId() + " 名字=" + student.getName());
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
