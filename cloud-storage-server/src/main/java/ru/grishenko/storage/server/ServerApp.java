package ru.grishenko.storage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class ServerApp {

    private static final int SERVER_PORT = 8189;

    public static void main(String[] args) {
        EventLoopGroup authGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        UserDAO userDAO = new UserDAO();

        try {
            ServerBootstrap sbs = new ServerBootstrap();
            sbs.group(authGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // инициализация подключающихся клиентов
                            socketChannel.pipeline().addLast(
//                                    new StringDecoder(),
//                                    new StringEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new ObjectCommandInputHandler(userDAO));
                        }
                    });
            ChannelFuture future = sbs.bind(SERVER_PORT).sync();
            future.channel().closeFuture().sync();      // ожидание закрытия сервера (блокирующее)
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            authGroup.shutdownGracefully();
        }
    }
}
