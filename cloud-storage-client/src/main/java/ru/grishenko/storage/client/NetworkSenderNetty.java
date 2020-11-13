package ru.grishenko.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ru.grishenko.storage.client.handler.ChunkedFileInbounHandler;
import ru.grishenko.storage.client.handler.CommandInboundHandler;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.client.helper.FileRequest;
import ru.grishenko.storage.client.interf.CallBack;

public class NetworkSenderNetty {

    private SocketChannel channel;
//    private CallBack onMessageReceivedCallBack;


    public NetworkSenderNetty(CallBack onMessageReceivedCallBack) {
//        this.onMessageReceivedCallBack = onMessageReceivedCallBack;
        Thread t = new Thread(() -> {
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                Bootstrap bsb = new Bootstrap();
                bsb.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                channel = socketChannel;
                                socketChannel.pipeline().addLast(
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                        new ObjectEncoder(),
//                                        new ChunkedFileInbounHandler(),
                                        new CommandInboundHandler(onMessageReceivedCallBack)

                                );
                            }
                        });
                ChannelFuture future = bsb.connect("localhost", 8189).sync();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void sendCommand(Command command) {
        channel.writeAndFlush(command);
    }

//    public void fileRequest(FileRequest fileRequest) {
//        channel.writeAndFlush()
//    }
}
