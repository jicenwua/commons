package com.xcz.commons.oss.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

/**
 * OSS路径工具类
 * 用于生成OSS文件存储路径和文件名
 */
public class OssPathUtil {
    
    /**
     * 生成带原文件名的UUID路径（保留原文件名）
     * 格式：原文件名-UUID.扩展名
     * @param fileName 原始文件名
     * @return 生成的文件路径
     */
    public static String fileNameUUidPath(String fileName){
        String originalName = fileName.substring(0, fileName.lastIndexOf("."));
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return originalName + "-" + UUID.randomUUID() + extension;
    }
    
    /**
     * 生成带原文件名的UUID路径，并添加指定目录
     * @param fileName 原始文件名
     * @param path 存储目录路径
     * @return 生成的完整文件路径
     */
    public static String fileNameUUidPath(String fileName,String path){
        return path + "/" + fileNameUUidPath(fileName);
    }
    
    /**
     * 从MultipartFile生成带原文件名的UUID路径
     * @param file 上传的文件
     * @return 生成的文件路径
     */
    public static String fileNameUUidPath(MultipartFile file){
        return fileNameUUidPath(Objects.requireNonNull(file.getOriginalFilename()));
    }
    
    /**
     * 从MultipartFile生成带原文件名的UUID路径，并添加指定目录
     * @param file 上传的文件
     * @param path 存储目录路径
     * @return 生成的完整文件路径
     */
    public static String fileNameUUidPath(MultipartFile file,String path){
        return path + "/" + fileNameUUidPath(file);
    }
    
    /**
     * 生成纯UUID路径（不保留原文件名）
     * 格式：UUID.扩展名
     * @param fileName 原始文件名
     * @return 生成的文件路径
     */
    public static String simpleUUidPath(String fileName){
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }
    
    /**
     * 生成纯UUID路径，并添加指定目录
     * @param fileName 原始文件名
     * @param path 存储目录路径
     * @return 生成的完整文件路径
     */
    public static String simpleUUidPath(String fileName, String path){
        return path + "/" + simpleUUidPath(fileName);
    }
    
    /**
     * 从MultipartFile生成纯UUID路径
     * @param file 上传的文件
     * @return 生成的文件路径
     */
    public static String simpleUUidPath(MultipartFile file){
        return simpleUUidPath(Objects.requireNonNull(file.getOriginalFilename()));
    }
    
    /**
     * 从MultipartFile生成纯UUID路径，并添加指定目录
     * @param file 上传的文件
     * @param path 存储目录路径
     * @return 生成的完整文件路径
     */
    public static String simpleUUidPath(MultipartFile file, String path){
        return path + "/" + simpleUUidPath(file);
    }
    
    /**
     * 根据文件名生成OSS存储路径（带日期目录）
     * 格式：yyyy/MM/dd/UUID.扩展名
     * @param fileName 原始文件名
     * @return 生成的文件路径
     */
    public static String generatePath(String fileName) {
        java.time.LocalDate now = java.time.LocalDate.now();
        String datePath = String.format("%d/%02d/%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return datePath + "/" + UUID.randomUUID() + extension;
    }
}
