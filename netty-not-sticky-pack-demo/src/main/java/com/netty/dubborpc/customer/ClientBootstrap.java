package com.netty.dubborpc.customer;

import com.netty.dubborpc.netty.NettyClient;
import com.netty.dubborpc.publicinterface.HelloService;

import java.lang.reflect.Proxy;

public class ClientBootstrap {

    //定义协议头
    public static final String PROTOCOL = "#";

    public static void main(String[] args) throws  Exception{

        //创建一个消费者
        NettyClient customer = new NettyClient();

        //创建代理对象
        HelloService service = (HelloService) customer.getBean(HelloService.class, PROTOCOL);

        for (;; ) {
            Thread.sleep(10 * 1000);

            //通过代理对象调用服务提供者的方法(服务)
            String res = service.hello("你好 dubbo~");
            System.out.println("调用的结果 res= " + res);
        }
    }
}
