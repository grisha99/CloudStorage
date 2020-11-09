package ru.grishenko.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ru.grishenko.storage.client.interf.CallBack;

public class NetworkSenderNetty {

    private SocketChannel channel;
    private CallBack onMessageReceivedCallBack;


    public NetworkSenderNetty(CallBack onMessageReceivedCallBack) {
        this.onMessageReceivedCallBack = onMessageReceivedCallBack;
        new Thread(() -> {
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                Bootstrap bsb = new Bootstrap();
                bsb.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                channel = socketChannel;
                                socketChannel.pipeline().addLast(new StringDecoder(), new StringEncoder(),
                                        new SimpleChannelInboundHandler<String>() {
                                            @Override
                                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
                                                if (onMessageReceivedCallBack != null) {
                                                    onMessageReceivedCallBack.callBack(s);
                                                }
                                                System.out.println("принял");
                                            }
                                        });
                            }
                        });
                ChannelFuture future = bsb.connect("localhost", 8189).sync();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        }).start();
    }

    public void sendCommand(String comm) {
        channel.writeAndFlush(comm);
    }
}
