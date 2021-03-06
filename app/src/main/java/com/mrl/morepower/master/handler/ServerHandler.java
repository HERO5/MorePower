package com.mrl.morepower.master.handler;

import android.os.Handler;

import com.mrl.morepower.master.data.ResourceRepository;
import com.mrl.morepower.master.manager.JobManager;
import com.mrl.netty.common.pojo.Task;
import com.mrl.netty.common.pojo.message.Message;
import com.mrl.netty.common.pojo.message.MessageContent;
import com.mrl.netty.common.pojo.message.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @program: com.mrl.netty.server.handler
 * @description: manage the workers
 * @author:
 * @create: 2020-02-01 15:04
 **/
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private Handler handler;

    public ServerHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String workerIp = insocket.getAddress().getHostAddress();
        int port = insocket.getPort();
        String workerId = workerIp+":"+port;
        Map<String, ChannelHandlerContext> workers = ResourceRepository.workers;
        workers.put(workerId+":"+port, ctx);
//        listener.handleReceive("有新连接接入 " + workerId+":"+port);
        sendMsg("有新连接接入 " + workerId, true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Map<String, ChannelHandlerContext> workers = ResourceRepository.workers;
        for (String workerId : workers.keySet()) {
            if (workers.get(workerId) == ctx) {
                workers.remove(workerId);
//                listener.handleReceive("断开连接 " + workerId);
                sendMsg("断开连接 " + workerId, true);
                return;
            }
        }
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String workerIp = insocket.getAddress().getHostAddress();
        int port = insocket.getPort();
        String workerId = workerIp+":"+port;
//        listener.handleReceive("断开连接失败" + workerId);
        sendMsg("断开连接失败" + workerId, true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String workerIp = insocket.getAddress().getHostAddress();
        int port = insocket.getPort();
        String workerId = workerIp+":"+port;
        Message message = (Message) msg;
        int type = message!=null?message.getMessageHeader().getMessageType():0;
        switch (type){
            case MessageType.TASK_GET:
                Task task = JobManager.getTask(workerId);
//                listener.handleReceive("来自"+workerId+"的任务请求:\n");
                sendMsg("来自"+workerId+"的任务请求", true);
                MessageContent<Task> content = new MessageContent<>(task);
                message.setMessageContent(content);
                message.getMessageHeader().setMessageType(MessageType.TASK_PUT);
                ctx.write(message);
                ctx.flush();
                break;
            case MessageType.TASK_SUBMIT:
                Object res = message.getMessageContent().getContent();
                boolean complete = JobManager.submitTask(workerId, res);
//                listener.handleReceive("来自"+workerId+"的提交: \n");
                sendMsg("来自"+workerId+"的提交: "+res+"; 结果正确："+complete+"\n", true);
                message.setMessageContent(null);
                message.getMessageHeader().setMessageType(MessageType.TASK_CONFIRM);
                ctx.write(message);
                ctx.flush();
                break;
            case MessageType.HEART_BEAT:
//                listener.handleReceive("来自客户端的心跳: "+ workerId);
                sendMsg("来自客户端的心跳: "+ workerId,true);
                break;
            default:
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state()== IdleState.READER_IDLE){
                InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
                String workerIp = insocket.getAddress().getHostAddress();
                int port = insocket.getPort();
                String workerId = workerIp+":"+port;
//                listener.handleReceive("关闭这个不活跃通道！"+workerId);
                sendMsg("关闭这个不活跃通道！"+workerId, true);
                ctx.channel().close();
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }

    private void sendMsg(String message, boolean isLog){
        android.os.Message msg = new android.os.Message();
        msg.obj = message;
        handler.sendMessage(msg);
        if (isLog){
            logger.info(message);
        }
    }
}
