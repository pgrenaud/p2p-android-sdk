package com.pgrenaud.android.p2p.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pgrenaud.android.p2p.entity.FileEntity;

public class FileRepository {

    private final Map<UUID, FileEntity> files;

    public FileRepository() {
        files = new ConcurrentHashMap<>();
    }

    public FileEntity get(UUID uuid) {
        return files.get(uuid);
    }

    public Collection<FileEntity> getAll() {
        return files.values();
    }

    public void add(FileEntity fileEntity) {
        files.put(fileEntity.getUuid(), fileEntity);
    }

    public void addAll(String path) {
        addAll(new File(path));
    }

    public void addAll(File directory) {
        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                add(new FileEntity(file));
            }
        }
    }

    public void remove(FileEntity fileEntity) {
        files.remove(fileEntity.getUuid());
    }

    public void removeAll() {
        files.clear();
    }

    public String encode() {
        Gson gson = new Gson();

        return gson.toJson(getAll());
    }

    public static Collection<FileEntity> decode(String json) throws JsonSyntaxException {
        Gson gson = new Gson();
        Type type = new TypeToken<Collection<FileEntity>>(){}.getType();

        return gson.fromJson(json, type);
    }
}
