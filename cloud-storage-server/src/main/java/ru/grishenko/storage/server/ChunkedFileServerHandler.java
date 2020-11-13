package ru.grishenko.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ChunkedFileServerHandler extends ChunkedWriteHandler {

    public ChunkedFileServerHandler(ChannelHandlerContext ctx, File file) {
        ChunkedFile chunkedFile;
        try {
            chunkedFile = new ChunkedFile(file);
            ctx.writeAndFlush(chunkedFile);
//            ctx.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("ChunkedFileServer active");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }


}
