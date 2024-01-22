package it.pagopa.pn.pecmock.test.endpoint.utils;

import jakarta.activation.DataSource;

import java.io.*;

public class ByteArrayDataSource implements DataSource {

    private final byte[] fileBytes;
    private String contentType;
    private String name;

    public ByteArrayDataSource(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileBytes);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
