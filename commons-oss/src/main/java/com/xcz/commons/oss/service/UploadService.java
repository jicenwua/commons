package com.xcz.commons.oss.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * OSS文件上传服务接口
 * 提供文件上传、进度监听和临时URL生成功能
 */
public interface UploadService {

    /**
     * 简单上传文件（不带进度）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成UUID路径）
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     * @throws IOException IO异常
     */
    String simpleUpload(MultipartFile file, String path) throws IOException;

    /**
     * 简单上传文件（不带进度）
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于生成存储路径）
     * @param path OSS中的存储路径（可选，为空时自动生成UUID路径）
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     */
    String simpleUpload(InputStream inputStream, String fileName, String path);

    /**
     * 带进度回调的文件上传
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param progressCallback 进度回调函数，参数为已上传字节数和总字节数
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     */
    String uploadWithProgress(InputStream inputStream, String fileName, long fileSize, String path, ProgressCallback progressCallback);

    /**
     * 带进度回调的文件上传（MultipartFile便捷方法）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param progressCallback 进度回调函数，参数为已上传字节数和总字节数
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     * @throws IOException IO异常
     */
    String uploadWithProgress(MultipartFile file, String path, ProgressCallback progressCallback) throws IOException;

    /**
     * 获取文件的临时访问URL（带有效期）
     * @param objectKey OSS中的存储路径
     * @return 带有效期的预签名URL，过期后需重新生成
     */
    String getUrl(String objectKey);

    public String getEnteralUrl(String path);

    /**
     * 分片上传文件（支持断点续传）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @return OSS中文件的存储路径（objectKey）
     * @throws IOException IO异常
     */
    String uploadChunked(MultipartFile file, String path) throws IOException;

    /**
     * 分片上传文件（支持断点续传，可指定覆盖策略）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param overwrite 是否覆盖已存在的文件（true=覆盖，false=生成新文件名）
     * @return OSS中文件的存储路径（objectKey）
     * @throws IOException IO异常
     */
    String uploadChunked(MultipartFile file, String path, boolean overwrite) throws IOException;

    /**
     * 检查文件是否已存在
     * @param objectKey OSS中的存储路径
     * @return true-文件存在，false-文件不存在
     */
    boolean exists(String objectKey);

    /**
     * 删除OSS中的文件
     * @param path OSS中的存储路径（objectKey）
     * @return true-删除成功或文件不存在，false-删除失败
     */
    boolean delete(String path);

    /**
     * 进度回调函数式接口
     * 用于实时接收文件上传进度信息
     */
    @FunctionalInterface
    interface ProgressCallback {
        /**
         * 进度更新回调
         * @param bytesWritten 已上传的字节数
         * @param totalBytes 总字节数
         */
        void onProgress(long bytesWritten, long totalBytes);
    }
}
