/**
 * netty.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.netty.hearbest.model;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;

@Data
public class RequestInfo implements Serializable {

    /**
     * 以ip为标识
     */
    private String ip;
    private HashMap<String, Object> cpuPercMap;
    private HashMap<String, Object> memoryMap;

}