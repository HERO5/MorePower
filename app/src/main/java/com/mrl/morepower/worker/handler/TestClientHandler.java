package com.mrl.morepower.worker.handler;

import android.util.ArrayMap;

import com.mrl.morepower.business.OnReceiveListener;
import com.mrl.morepower.master.handler.Test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by user on 2016/10/27.
 */

public class TestClientHandler extends SimpleChannelInboundHandler<Test.ProtoTest> {
    private ArrayMap<Integer, OnReceiveListener> receiveListenerHolder;

    public TestClientHandler() {
        receiveListenerHolder = new ArrayMap<>();
    }

    public void holdListener(Test.ProtoTest test, OnReceiveListener onReceiveListener) {
        receiveListenerHolder.put(test.getId(), onReceiveListener);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Test.ProtoTest protoTest) throws Exception {
        if (receiveListenerHolder.containsKey(protoTest.getId())) {
            OnReceiveListener listener = receiveListenerHolder.remove(protoTest.getId());
            if (listener != null) {
                listener.handleReceive(protoTest);
            }
        }
    }
}
