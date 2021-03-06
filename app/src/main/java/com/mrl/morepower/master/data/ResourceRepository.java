package com.mrl.morepower.master.data;

import com.mrl.netty.common.pojo.Task;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @program: com.mrl.netty.server.pojo
 * @description: hold all the alive channel
 * @author:
 * @create: 2020-02-01 14:58
 **/
public class ResourceRepository {

    //worker清单<workerIp, ctx>
    public static final Map<String, ChannelHandlerContext> workers = new ConcurrentHashMap<>();

    //未完成任务清单
    public static final List<Task> task = new CopyOnWriteArrayList<>();

    //任务分配清单<workerIp:port, task>
    public static final Map<String, Task> taskWorker = new ConcurrentHashMap<>();

    //已完成任务清单<task.id, result>
    public static final Map<String, Object> taskResult = new ConcurrentHashMap<>();

}
