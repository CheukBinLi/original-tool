package com.github.cheukbinli.original.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IoUtils {

    static final byte[] EMPTY = new byte[0];

    public static void write(InputStream in, OutputStream out) throws IOException {
        byte[] buff = new byte[2408];
        int count = 0;
        while ((count = in.read(buff)) > 0) {
            out.write(buff, 0, count);
        }
    }

    public static void write(ByteArrayOutputStream in, OutputStream out) throws IOException {
        out.write(in.toByteArray());
    }

    public static byte[] read(InputStream in, int offset, int len) throws IOException {
        if (null == in)
            return EMPTY;
        byte[] buff = new byte[len];
        in.read(buff, offset, len);
        return buff;
    }

    public static void write(byte[] data, OutputStream out) throws IOException {
        out.write(data);
    }

    public static void write(String str, String charset, OutputStream out) throws IOException {
        out.write(str.getBytes(null == charset ? "UTF-8" : charset));
    }

}
