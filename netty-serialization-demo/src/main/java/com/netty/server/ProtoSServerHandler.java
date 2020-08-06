package com.netty.server;

import com.netty.protobuf.StudentPOJO;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Netty 自身提供了一些 codec（编解码器），其编码解码的机制和问题分析：
 * Netty 提供的编码器：StringEncoder 对字符串数据进行编码，ObjectEncoder 对 Java 对象进行编码，等等。
 * Netty 提供的解码器：StringDecoder 对字符串数据进行解码，ObjectDecoder 对 Java 对象进行解码，等等。
 *
 * Netty 本身自带的 ObjectDecoder 和 ObjectEncoder 可以用来实现 POJO 对象或各种业务对象的编码和解码，底层使用的仍是 Java 序列
 * 化技术，而Java 序列化技术本身效率就不高，存在如下问题：无法跨语言（客户端服务端必须都是 java 编写），序列化后的体积太大，是二
 * 进制编码的 5 倍多。序列化性能太低。由此引出新的解决方案  Google 的 Protobuf。
 * 参考文档 : https://developers.google.com/protocol-buffers/docs/proto。
 *
 * Protobuf 是 Google 发布的开源项目，全称 Google Protocol Buffers，是一种轻便高效的结构化数据存储格式，可以用于结构化数据串行
 * 化，或者说序列化。它很适合做数据存储或 RPC 远程过程调用的数据交换格式 。目前很多公司都由 http+json 转换为使用 tcp+protobuf。
 *
 * Protobuf 是以 message 的方式来管理数据的。支持跨平台、跨语言，即客户端和服务器端可以是不同的语言编写的（支持目前绝大多数语言，
 * 例如 C++、C#、Java、python 等），具有高性能，高可靠性。
 * 并且使用 protobuf 编译器能自动生成代码，即 Protobuf 是将类的定义使用 .proto 文件进行描述。并且在 idea 中编写 .proto 文件时，
 * 会自动提示是否下载 .ptotot 编写插件，可以让语法高亮。编写完成之后，使用从官网下载的 proto 压缩包中 bin/protoc.exe 编译器根据
 * .proto 文件自动生成 .java 文件。首先将 .proto 文件放到与 protoc.exe 同一文件夹下（bin），然后在当前文件夹执行 cmd 命令，
 * protoc.exe --java_out=. xxx.proto（点和文件名之间有空格）
 *
 * 客户端业务数据 y.proto -> protoc.exe -> x.java -> ProtobufEncoder 编码 -> 传输二进制字节码 -> 服务端 ProtobufDecoder 解码
 * -> 业务数据对象 -> 使用。
 *
 *
 * 1. 我们自定义一个 Handler 需要继续采用 netty 规定好的某个HandlerAdapter(规范)
 * 2. 这时我们自定义一个Handler , 才能称为一个handler
 */
//public class NettyServerHandler extends ChannelInboundHandlerAdapter {
public class ProtoSServerHandler extends SimpleChannelInboundHandler<StudentPOJO.Student> {

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
