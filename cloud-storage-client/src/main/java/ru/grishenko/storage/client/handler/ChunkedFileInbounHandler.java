package ru.grishenko.storage.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChunkedFileInbounHandler extends ChunkedWriteHandler/*SimpleChannelInboundHandler<ChunkedFile>*/ {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("chunked active");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("chunked read");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

//    @Override
//    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ChunkedFile chunkedFile) throws Exception {
//        System.out.println("chunked read00");
//    }
}
