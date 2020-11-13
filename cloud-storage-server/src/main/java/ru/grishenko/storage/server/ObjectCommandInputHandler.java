package ru.grishenko.storage.server;

import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.server.exception.*;
import ru.grishenko.storage.server.helper.AuthInfo;
import ru.grishenko.storage.server.helper.Navigate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ObjectCommandInputHandler extends ChannelInboundHandlerAdapter/*SimpleChannelInboundHandler <String>*/ {

    private UserDAO userDAO;
//    private static final List<Channel> channels = new ArrayList<>();
    private Path userHome;
    private static final Path ROOT_PATH = Paths.get("");
    private Path currentDir;


    public ObjectCommandInputHandler(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
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
                        } else {
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не правильный логин, пароль."));
                        }
                    } catch (UserLoginException
                            | UserPassException
                            | DBConnectException
                            | ReadResultSetException e) {
                        System.out.println(e.getMessage());
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
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.AUTH_OK, currentDir.toString()));
                    } catch (UserExistsException e) {
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
                        } else {
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Файл с таким именем уже существует."));
                        }
                    } else {
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Имя файла не указано"));
                    }
                    break;
                }

                case MKDIR: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path newDirPath = currentDir.resolve(cmd.getArgs()[0]);
                        if (!Files.exists(newDirPath)) {
                            Files.createDirectory(newDirPath);
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Каталог с таким именем уже существует."));
                        }
                    } else {
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
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Объекта с таким именем уже сущесвует"));

                        }
                    } else {
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не верные параметру переименования"));
                    }
                    break;
                }

                case DEL: {
                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
                        Path toDeleteFile = currentDir.resolve(cmd.getArgs()[0]);
                        if (Files.exists(toDeleteFile)) {
                            Files.delete(toDeleteFile);
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.FILE_OPERATION_OK));
                        } else {
                            ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Объекта с таким именем не найден"));
                        }
                    } else {
                        ctx.channel().writeAndFlush(Command.generate(Command.CommandType.ERROR, "Не указано имя объекта"));
                    }
                    break;
                }

                case COPY: {
//                    if (cmd.getArgs().length > 0 && cmd.getArgs()[0] != null && !cmd.getArgs()[0].equals("")) {
//                        Path filePath = currentDir.resolve(cmd.getArgs()[0]);
//                        if (Files.exists(filePath)) {
//                            if (!Files.isDirectory(filePath)) {
//                                FileRequest fr = new FileRequest(filePath.getFileName().toString());
//                                fr.setFileSize(Files.size(filePath));
//                                ctx.channel().writeAndFlush(fr);
//                                new ChunkedFileServerHandler(ctx, new File(filePath.toString()));
//                            }
//                        }
//                    }
                    break;
                }
            }
        }

    }
}
