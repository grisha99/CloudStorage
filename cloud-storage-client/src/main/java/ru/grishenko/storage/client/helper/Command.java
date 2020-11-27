package ru.grishenko.storage.client.helper;

import java.io.Serializable;

public class Command implements Serializable {

    public enum CommandType {
        AUTH("/auth %s %s"),                    // запрос авторизации
        REGISTER("/reg %s %s"),                 // запрос регистрации

        LIST("/ls"),                            // список фалой в каталоге
        MKDIR("/mkdir %s"),                     // создать каталог
        TOUCH("/touch %s"),                     // создать файл
        REN("/ren %s %s"),                      // переименовать файл/каталог
        DEL("/del %s"),                         // удалить файл/каталог
        CD("/cd %s"),                           // переход в директорию
        CD_UP("/cd.."),                         // переход науровень выше
        CD_HOME("/home"),                       // переход в домашнюю директорию
        CD_ROOT("/root"),                       // переход в корневую директорию
        COPY("/copy %s"),                       // копирование файла
        MOVE("/move %s"),                       // перемещение файла

        SWITCH_FEED_BACK("/noFeedBack"),        // переключение сервера в режим "без ответа" при копировании каталогов

        AUTH_OK("/authOK %s"),                  // удачная авторизация
        CD_OK("/cdOK %s"),                      // удачный переход в директорию
        FILE_OPERATION_OK("/fOperationOK %s"),  // удачная операция с фалами (создание, переименованиеб удаление)

        ERROR("/error %s");                     // ошибка

        private String commandName;

        public String getCommandName() {
            return commandName;
        }

        CommandType(String commandName) {
            this.commandName = commandName;
        }
    }

    private CommandType type;
    private String [] args;

    public static Command generate(CommandType type, String... args) {
        return new Command(type, args);
    }

    private Command(CommandType type, String... args) {
        this.type = type;
        this.args = args;
    }

    public CommandType getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return String.format(type.commandName, args);
    }
}
