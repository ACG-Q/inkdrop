package com.example.wallpaper.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件操作工具类
 * 提供安全的文件读写操作，确保资源正确关闭
 */
public final class FileUtils {
    
    private FileUtils() {
        // 工具类不允许实例化
    }
    
    /**
     * 读取文件内容为字符串
     *
     * @param file 要读取的文件
     * @return 文件内容字符串
     * @throws IOException 如果读取失败
     */
    public static String readFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("文件不存在: " + file);
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }
    
    /**
     * 读取 InputStream 内容为字符串
     *
     * @param inputStream 输入流
     * @return 内容字符串
     * @throws IOException 如果读取失败
     */
    public static String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("InputStream 为空");
        }
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }
    
    /**
     * 将字符串写入文件
     *
     * @param file    目标文件
     * @param content 要写入的内容
     * @throws IOException 如果写入失败
     */
    public static void writeFile(File file, String content) throws IOException {
        if (file == null) {
            throw new IOException("文件路径为空");
        }
        
        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes("UTF-8"));
        }
    }
    
    /**
     * 将字节数组写入文件
     *
     * @param file  目标文件
     * @param data  要写入的字节数组
     * @throws IOException 如果写入失败
     */
    public static void writeBytes(File file, byte[] data) throws IOException {
        if (file == null) {
            throw new IOException("文件路径为空");
        }
        
        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
    
    /**
     * 复制文件
     *
     * @param source 源文件
     * @param dest   目标文件
     * @throws IOException 如果复制失败
     */
    public static void copyFile(File source, File dest) throws IOException {
        if (source == null || !source.exists()) {
            throw new IOException("源文件不存在: " + source);
        }
        
        // 确保目标目录存在
        File parentDir = dest.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }
    }
}
