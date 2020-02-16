package com.mrl.morepower.worker.handler;

import android.os.Handler;

import com.mrl.morepower.MainActivity;
import com.mrl.netty.common.pojo.Task;
import com.mrl.netty.common.pojo.message.Message;
import com.mrl.netty.common.pojo.message.MessageType;
import com.mrl.netty.common.utils.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import static com.mrl.morepower.MainActivity.factory;

/**
 * @program: com.mrl.netty.client.handler
 * @description:
 * @author:
 * @create: 2020-02-01 18:53
 **/
public class WorkerHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(WorkerHandler.class);

    private Handler handler;

    public WorkerHandler(Handler handler) {
        this.handler = handler;
    }


    //通道就绪事件
    @Override
    public void channelActive(ChannelHandlerContext ctx){

        sendMsg("client login", true);
        Message message = Message.TASK_GET;
        //Message message = Message.HEART_BEAT;
        ctx.writeAndFlush(message);
    }

    //读取数据事件
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg){
        Message message=(Message) msg;
        sendMsg("服务器端发来的消息："+message.getMessageHeader().toString(), true);
        int type = message!=null?message.getMessageHeader().getMessageType():0;
        switch (type){
            case MessageType.TASK_PUT:
                Task task = (Task) message.getMessageContent().getContent();
                if(task!=null){
                    //设置编译参数
                    try {
                        FileUtil.writeFile(MainActivity.FILE_DIR+"/"+task.getName()+".py", task.getFunc());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Map data = task.getParams();
                    Object result = factory.call(task.getName(), task.getFuncName(), new Object[]{new Double((Double) data.get("f1")), new Double((Double) data.get("f2"))});
                    FileUtil.delFile(MainActivity.FILE_DIR+"/"+task.getName()+".py");
                    //输出结果
                    sendMsg("任务处理: "+task.getName()+"."+task.getFuncName()+"("+data.get("f1")+", "+data.get("f2")+")"+" -> "+result, true);
                    Message res = Message.SUBMIT;
                    res.getMessageContent().setContent(result);
                    ctx.writeAndFlush(res);
                }
                break;
            case MessageType.TASK_CONFIRM:
                sendMsg("结果得到确认", true);
                ctx.writeAndFlush(Message.TASK_GET);
                break;
            default:
                ctx.writeAndFlush(Message.TASK_GET);
                break;
        }
    }

    //用于捕获IdleState#WRITER_IDLE事件（未在指定时间内向服务器发送数据），然后向Server发送一个心跳包
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        long time = new Date().getTime();
        sendMsg("客户端循环心跳监测发送: "+time, true);
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                // write heartbeat to server
                ctx.writeAndFlush(Message.HEART_BEAT);
            }
        } else {
            super.userEventTriggered(ctx, evt);
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
