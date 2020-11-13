package ru.grishenko.storage.server.helper;

import ru.grishenko.storage.client.helper.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Navigate {

    public static List<FileInfo> getFileList(Path currentPath, Path homePath) throws IOException {
        List<FileInfo> fileList = Files.list(currentPath).map(FileInfo :: new).collect(Collectors.toList());
        if (!currentPath.toString().equals(homePath.toString())) {
            fileList.add(0, new FileInfo());
        }
        return fileList;
    }

}
