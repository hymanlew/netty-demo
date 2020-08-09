package com.netty.dubborpc.provider;

import com.netty.dubborpc.publicinterface.HelloService;

public class HelloServiceImpl implements HelloService{

    private static int count = 0;

    /**
     * 当有消费方调用该方法时，就返回一个结果
     *
     * @param mes
     * @return
     */
    @Override
    public String hello(String mes) {
        System.out.println("收到客户端消息=" + mes);

        // 根据 mes 返回不同的结果
        if(mes != null) {
            return "你好客户端, 我已经收到你的消息 [" + mes + "] 第" + (++count) + " 次";
        } else {
            return "你好客户端, 我已经收到你的消息 ";
        }
    }
}
