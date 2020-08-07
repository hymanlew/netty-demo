/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.fst.codec;

import org.nustaq.serialization.FSTConfiguration;

public class FstSerializer {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    /**
     * 反序列化
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        return (T) conf.asObject(data);
    }

    /**
     * 序列化
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> byte[] serialize(T obj) {
        return conf.asByteArray(obj);
    }
}