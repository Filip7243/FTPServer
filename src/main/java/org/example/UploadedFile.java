package org.example;

public class UploadedFile {

    private int id;
    private String name;
    private byte[] data;
    private String extension;

    public UploadedFile(int id, String name, byte[] data, String extension) {
        this.id = id;
        this.name = name;
        this.data = data;
        this.extension = extension;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
