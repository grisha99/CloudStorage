package ru.grishenko.storage.client.interf;

import java.io.IOException;

@FunctionalInterface
public interface CallBack {

    void callBack(Object ... args);
}
