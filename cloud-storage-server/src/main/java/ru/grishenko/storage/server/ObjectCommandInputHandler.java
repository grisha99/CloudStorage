package ru.grishenko.storage.server;

import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.client.helper.FileWrapper;
import ru.grishenko.storage.server.exception.*;
import ru.grishenko.storage.server.helper.AuthInfo;
import ru.grishenko.storage.server.helper.Navigate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ObjectCommandInputHandler extends ChannelInboundHandlerAdapter/*SimpleChannelInboundHandler <String>*/ {

    private static final Logger LOGGER = LogManager.getLogger(ObjectCommandInputHandler.class.getName());

    private UserDAO userDAO;
    private Path userHome;
    private static final Path ROOT_PATH = Paths.get("");
    private Path currentDir;
    private FileOutputStream fos;


    public ObjectCommandInputHandler(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.log(Level.INFO, "Client connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.log(Level.INFO, "Client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        LOGGER.log(Level.ERROR, cause.getMessage());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof Command) {
            Command cmd = (Command) msg;
            switch (cmd.getType()) {
                case AUTH: {
                    try {
                        AuthInfo authInfo = new AuthInfo(cmd.getArgs());

                        if (userDAO.checkUser(authInfo.getName(), authInfo.getPass())) {
                            userHome = ROOT_PATH.resolve(authInfo.getName());
                            if (!Files.exists(userHome)) {
                                Files.createDirectory(userHome);
                            }
                            currentDir = Paths.get(userHome.toString());

                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.AUTH_OK, currentDir.toString()));
                            LOGGER.log(Level.INFO, "Client AUTHORIZED by \"" + authInfo.getName() + "\"");
                        } else {
                            LOGGER.log(Level.ERROR, "Login and Pass WRONG");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не правильный логин, пароль."));
                        }
                    } catch (UserLoginException
                            | UserPassException
                            | DBConnectException
                            | ReadResultSetException e) {
                        LOGGER.log(Level.ERROR, e.getMessage());
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, e.getMessage()));
                    }
                    break;
                }

                case REGISTER: {
                    try {
                        userDAO.registerUser(cmd.getArgs()[0], cmd.getArgs()[1]);
                        userHome = ROOT_PATH.resolve(cmd.getArgs()[0]);
                        if (!Files.exists(userHome)) {
                            Files.createDirectory(userHome);
                        }
                        currentDir = Paths.get(userHome.toString());
                        LOGGER.log(Level.INFO, "Client REGISTER by \"" + cmd.getArgs()[0] + "\"");
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.AUTH_OK, currentDir.toString()));
                    } catch (UserExistsException e) {
                        LOGGER.log(Level.ERROR, e.getMessage());
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, e.getMessage()));
                    }
                    break;
                }

                case LIST: {
                    ctx.channel().writeAndFlush(Navigate.getFileList(currentDir, userHome));
                    break;
                }
                case CD: {
                    currentDir = currentDir.resolve(cmd.getArgs()[0].split("\\s")[0]);
                    ctx.channel().writeAndFlush(Command.generate(Command.CommandType.CD_OK, currentDir.toString()));
                    break;
                }

                case UPCD: {
                    if (currentDir.getParent() != null) {
                        currentDir = currentDir.getParent();
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.CD_OK, currentDir.toString()));
                    } else {
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Достигнута ваша домашняя директория."));
                    }
                    break;
                }

                case TOUCH: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path newFilePath = currentDir.resolve(cmd.getArgs()[0]);
                        if (!Files.exists(newFilePath)) {
                            Files.createFile(newFilePath);
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                            LOGGER.log(Level.INFO, "File CREATE by name \"" + cmd.getArgs()[0] + "\"");
                        } else {
                            LOGGER.log(Level.ERROR, "File \"" + cmd.getArgs()[0] + "\" already exists");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Файл с таким именем уже существует."));
                        }
                    } else {
                        LOGGER.log(Level.ERROR, "File name is BLANK");
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Имя файла не указано"));
                    }
                    break;
                }

                case MKDIR: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path newDirPath = currentDir.resolve(cmd.getArgs()[0]);
                        if (!Files.exists(newDirPath)) {
                            Files.createDirectory(newDirPath);
                            LOGGER.log(Level.INFO, "Folder CREATE by name \"" + cmd.getArgs()[0] + "\"");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            LOGGER.log(Level.ERROR, "Folder \"" + cmd.getArgs()[0] + "\" already exists");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Каталог с таким именем уже существует."));
                        }
                    } else {
                        LOGGER.log(Level.ERROR, "Folder name is BLANK");
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Имя каталога не указано"));
                    }
                    break;
                }

                case REN: {
                    if (cmd.getArgs().length > 1
                            && cmd.getArgs()[0] != null
                            && !cmd.getArgs()[1].equals("")
                            && cmd.getArgs()[1] != null) {
                        Path oldFilePath = currentDir.resolve(cmd.getArgs()[0]);
                        Path newFilePath = currentDir.resolve(cmd.getArgs()[1]);
                        if (!Files.exists(newFilePath) && Files.exists(oldFilePath)) {
                            Files.move(oldFilePath, newFilePath);
                            LOGGER.log(Level.INFO, "Rename successful:  Old name \"" + cmd.getArgs()[0] + "\", New name\"" + cmd.getArgs()[1] + "\"");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            LOGGER.log(Level.ERROR, "Object \"" + cmd.getArgs()[0] + "\" already exists");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Объекта с таким именем уже сущесвует"));

                        }
                    } else {
                        LOGGER.log(Level.ERROR, "Object name is BLANK");
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не верные параметру переименования"));
                    }
                    break;
                }

                case DEL: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path toDeleteFile = currentDir.resolve(cmd.getArgs()[0]);
                        if (Files.exists(toDeleteFile)) {
                            Files.delete(toDeleteFile);
                            LOGGER.log(Level.INFO, "Object \"" + cmd.getArgs()[0] + "\" DELETE successful");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            LOGGER.log(Level.ERROR, "Object \"" + cmd.getArgs()[0] + "\" not found");
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Объекта с таким именем не найден"));
                        }
                    } else {
                        LOGGER.log(Level.ERROR, "Object name is BLANK");
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не указано имя объекта"));
                    }
                    break;
                }

                case COPY: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path filePath = currentDir.resolve(cmd.getArgs()[0]);
                        FileInputStream fis = new FileInputStream(filePath.toString());
                        int read;
                        int part = 1;
                        FileWrapper fw = new FileWrapper(filePath, Command.CommandType.COPY);
                        LOGGER.log(Level.INFO, "Object \"" + cmd.getArgs()[0] + "\" ready to transfer");
                        while ((read = fis.read(fw.getBuffer())) != -1) {
                            fw.setCurrentPart(part);
                            fw.setReadByte(read);
                            ctx.channel().writeAndFlush(fw).sync();
                            part++;
                        }
                        LOGGER.log(Level.INFO, "Send complete: " + fw.toString());
                        fis.close();
                    }
                    break;
                }
            }
        }

        if (msg instanceof FileWrapper) {
            FileWrapper fw = (FileWrapper) msg;
            if (fw.getType() == Command.CommandType.COPY) {
                File file = new File(currentDir.resolve(fw.getFileName()).toString());
                if (!file.exists()) {
                    file.createNewFile();
                }
                if (fos == null) {
                    fos = new FileOutputStream(file);

                }
                if (fos.getChannel().isOpen()) {
                    fos.write(fw.getBuffer(),0,fw.getReadByte());
                } else {
                    fos = new FileOutputStream(file);
                    fos.write(fw.getBuffer(), 0, fw.getReadByte());

                }
                if (fw.getCurrentPart() == fw.getParts()) {
                    fos.close();
                    LOGGER.log(Level.INFO, "Object RECEIVED: " + fw.getFileName());
                    ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                }

            }
        }

    }
}
