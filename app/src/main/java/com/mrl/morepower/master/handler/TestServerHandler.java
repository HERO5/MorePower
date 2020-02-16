package com.mrl.morepower.master.handler;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by user on 2016/10/27.
 */

public class TestServerHandler extends SimpleChannelInboundHandler<Test.ProtoTest> {
    private static final String TAG = "TestServerHandler";
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Test.ProtoTest protoTest) throws Exception {
        Log.d(TAG, "channelRead0: " + channelHandlerContext.name());
        Test.ProtoTest res = Test.ProtoTest.newBuilder()
                .setId(protoTest.getId())
                .setTitle("res" + protoTest.getTitle())
                .setContent("res" + protoTest.getContent())
                .build();
        channelHandlerContext.writeAndFlush(res);
    }
}
