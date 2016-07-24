package com.pgrenaud.android.p2p.entity;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class FileEntity implements Comparable<FileEntity> {

    private static final String TO_STRING_FORMAT = "%S (%s)";

    // Do not serialize file
    private transient File file;

    private UUID uuid;
    private String name;
    private Long size;

    public FileEntity(String path) {
        this(new File(path));
    }

    public FileEntity(String path, String name) {
        this(new File(path, name));
    }

    public FileEntity(File file) {
        this.file = file;

        name = file.getName();
        size = file.length();

        uuid = UUID.nameUUIDFromBytes(generateNameForUUID());
    }

    private byte[] generateNameForUUID() {
        return file.getAbsolutePath().getBytes();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    /**
     * Use {@link android.text.format.Formatter#formatFileSize} to render the human readable
     *
     * @see <a href="http://stackoverflow.com/a/26502430">http://stackoverflow.com/a/26502430</a>
     */
    public Long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), TO_STRING_FORMAT, uuid, name);
    }

    @Override
    public int compareTo(@NonNull FileEntity fileEntity) {
        return getName().compareTo(fileEntity.getName());
    }

    public String encode() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }

    public static FileEntity decode(String json) throws JsonSyntaxException {
        Gson gson = new Gson();

        return gson.fromJson(json, FileEntity.class);
    }
}
