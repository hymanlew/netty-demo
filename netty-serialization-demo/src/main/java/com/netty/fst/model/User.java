/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.fst.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 传输测试的实体类
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = -5135011481747489263L;
    private String username;
    private String password;
    private Integer age;
}