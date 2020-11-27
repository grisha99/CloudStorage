package ru.grishenko.storage.server.helper;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.client.helper.FileWrapper;
import ru.grishenko.storage.server.ObjectCommandInputHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {

    private ChannelHandlerContext ctx;
    private Path elementPath;
    private Command cmd;
    private boolean isDeepCopy;
    private static final Logger LOGGER = LogManager.getLogger(ObjectCommandInputHandler.class);


    public FileSender(ChannelHandlerContext ctx, Path elementPath, Command cmd) throws IOException, InterruptedException {
        this.ctx = ctx;
        this.elementPath = elementPath;
        this.cmd = cmd;
        if (Files.isDirectory(elementPath)) {
            isDeepCopy = true;
            sendDir();
        } else {
            isDeepCopy = false;
            sendFile(this.elementPath);
        }
    }

//    public static void send(ChannelHandlerContext ctx, Path elementPath) throws IOException, InterruptedException {
//
//    }

    private void sendFile(Path filePath) throws IOException, InterruptedException {
        FileInputStream fis = new FileInputStream(filePath.toString());
        FileWrapper fw;
        try {
            int read;
            int part = 1;

            fw = new FileWrapper(filePath, Command.CommandType.COPY, isDeepCopy);

            LOGGER.log(Level.INFO, "Object \"" + filePath + "\" ready to transfer");
            while ((read = fis.read(fw.getBuffer())) != -1) {
                fw.setCurrentPart(part);
                fw.setReadByte(read);
                ctx.channel().writeAndFlush(fw).sync();
                part++;
            }
            LOGGER.log(Level.INFO, "Send complete: " + fw.toString());
        } finally {
            fis.close();
        }

//        if (cmd.getType() == Command.CommandType.MOVE) {
//            Files.delete(elementPath);
//            LOGGER.log(Level.INFO, "Delete after send complete: " + fw.toString());
//            if (needFeedBack) {
//                ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
//            }
//        }
    }

    private void sendDir()  throws IOException {
        Files.walk(elementPath).forEach(item -> {
            if (Files.isDirectory(item)) {
                try {
                    ctx.channel().writeAndFlush(new FileWrapper(item, cmd.getType(), isDeepCopy)).sync();
                } catch (IOException | InterruptedException e) {
                    LOGGER.log(Level.ERROR, e);
                }
            } else {
                try {
                    sendFile(item);
                } catch (InterruptedException | IOException e) {
                    LOGGER.log(Level.ERROR, e);
                }
            }
        });
    }
}
