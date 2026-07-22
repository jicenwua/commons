package com.xcz.commons.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.StringUtils;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadFileRequest;
import com.xcz.commons.oss.config.OssProperties;
import com.xcz.commons.oss.listener.PutObjectProgressListener;
import com.xcz.commons.oss.service.UploadService;
import com.xcz.commons.oss.util.OssPathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Date;

/**
 * OSS文件上传服务实现类
 * 提供简单上传、带进度上传和临时URL生成功能
 */
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final OSS ossClient;
    private final OssProperties properties;

    /**
     * 获取文件的临时访问URL（带有效期）
     * @param objectKey OSS中的存储路径
     * @return 带有效期的预签名URL，过期后需重新生成
     */
    @Override
    public String getUrl(String objectKey){
        // 生成带有效期的预签名URL，用于安全访问OSS文件
        if (StringUtils.isNullOrEmpty(objectKey)) {
            return null;
        }

        objectKey = objectKey.trim();
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        Date expiration = new Date(System.currentTimeMillis() + properties.getExpireTime());
        URL url = ossClient.generatePresignedUrl(properties.getBucketName(), objectKey, expiration);
        return url.toString();
    }

    /**
     * 生成永久url
     * @param path  保存i地址
     * @return  url
     */
    public String getEnteralUrl(String path){
        return "https://" +properties.getBucketName() + "."+ properties.getEndpoint() + "/" + path;
    }

    /**
     * 简单上传文件（不带进度）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成UUID路径）
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     * @throws IOException IO异常
     */
    @Override
    public String simpleUpload(MultipartFile file, String path) throws IOException {
        return simpleUpload(file.getInputStream(), file.getOriginalFilename(), path);
    }

    /**
     * 简单上传文件（不带进度）
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于生成存储路径）
     * @param path OSS中的存储路径（可选，为空时自动生成UUID路径）
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     */
    @Override
    public String simpleUpload(InputStream inputStream, String fileName, String path){
        try {
            if(path == null || path.isEmpty()){
                path = OssPathUtil.simpleUUidPath(fileName);
            }else if(exists(path)) {
                path = nonePath(path);
            }

            // 上传文件到OSS
            ossClient.putObject(properties.getBucketName(), path, inputStream);

            // 返回文件的OSS存储路径
            return path;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带进度回调的文件上传
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param progressCallback 进度回调函数
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     */
    @Override
    public String uploadWithProgress(InputStream inputStream, String fileName, long fileSize, String path, ProgressCallback progressCallback) {
        try {
            if(path == null || path.isEmpty()){
                // 根据文件名生成带日期目录的OSS存储路径
                path = OssPathUtil.fileNameUUidPath(fileName);
            }else if(exists(path)){
                path = nonePath(path);
            }

            // 创建 ObjectMetadata 并设置文件大小（关键！否则进度回调不会触发）
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(fileSize);

            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), path, inputStream, meta);

            // 添加进度监听器
            if (progressCallback != null) {
                putObjectRequest.setProgressListener(new PutObjectProgressListener(progressCallback));
            }

            // 执行上传
            ossClient.putObject(putObjectRequest);

            // 返回文件的OSS存储路径
            return path;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带进度回调的文件上传（MultipartFile便捷方法）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param progressCallback 进度回调函数
     * @return OSS中文件的存储路径（objectKey），可用于后续生成临时访问URL
     * @throws IOException IO异常
     */
    @Override
    public String uploadWithProgress(MultipartFile file, String path, ProgressCallback progressCallback) throws IOException {
        return uploadWithProgress(
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getSize(),
            path,
            progressCallback
        );
    }

    /**
     * 分片上传文件（支持 MultipartFile）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @return OSS中文件的存储路径（objectKey）
     */
    @Override
    public String uploadChunked(MultipartFile file, String path) {
        return uploadChunked(file, path, false); // 默认不覆盖，生成新文件名
    }

    /**
     * 分片上传文件（支持 MultipartFile，可指定覆盖策略）
     * @param file 上传的文件
     * @param path OSS中的存储路径（可选，为空时自动生成日期目录路径）
     * @param overwrite 是否覆盖已存在的文件（true=覆盖，false=生成新文件名）
     * @return OSS中文件的存储路径（objectKey）
     */
    @Override
    public String uploadChunked(MultipartFile file, String path, boolean overwrite) {
        try {
            if(path == null || path.isEmpty()){
                // 根据文件名生成带UUID的OSS存储路径（避免冲突）
                path = OssPathUtil.fileNameUUidPath(file);
            } else if (!overwrite && exists(path)) {
              path = nonePath( path);
            }

            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(file.getContentType());

            // 通过UploadFileRequest设置多个参数。
            UploadFileRequest uploadFileRequest = new UploadFileRequest(properties.getBucketName(), path);

            // 创建临时文件用于分片上传
            java.io.File tempFile = java.io.File.createTempFile("upload_", "_" + file.getOriginalFilename() + System.currentTimeMillis());
            file.transferTo(tempFile);

            // 通过UploadFileRequest设置单个参数。
            // 填写本地文件的完整路径
            uploadFileRequest.setUploadFile(tempFile.getAbsolutePath());
            // 指定上传并发线程数，默认值为1。
            uploadFileRequest.setTaskNum(5);
            // 指定上传的分片大小，单位为字节，取值范围为100 KB~5 GB。默认值为100 KB。
            uploadFileRequest.setPartSize(5 * 1024 * 1024); // 5MB
            // 开启断点续传，默认关闭。
            uploadFileRequest.setEnableCheckpoint(true);
            // 记录本地分片上传结果的文件。上传过程中的进度信息会保存在该文件中，如果某一分片上传失败，再次上传时会根据文件中记录的点继续上传。上传完成后，该文件会被删除。
            // 如果未设置该值，默认与待上传的本地文件同路径，名称为${uploadFile}.ucp。
            String checkpointFile = tempFile.getAbsolutePath() + ".ucp";
            uploadFileRequest.setCheckpointFile(checkpointFile);
            // 文件的元数据。
            uploadFileRequest.setObjectMetadata(meta);

            // 断点续传上传。
            ossClient.uploadFile(uploadFileRequest);

            // 上传完成后删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            // 删除检查点文件
            java.io.File cpFile = new java.io.File(checkpointFile);
            if (cpFile.exists()) {
                cpFile.delete();
            }

            // 返回文件的OSS存储路径
            return path;
        }  catch (Throwable e) {
            throw new RuntimeException("分片上传失败：" + e.getMessage());
        }
    }

    /**
     * 检查文件是否已存在
     * @param objectKey OSS中的存储路径
     * @return true-文件存在，false-文件不存在
     */
    @Override
    public boolean exists(String objectKey) {
        try {
            return ossClient.doesObjectExist(properties.getBucketName(), objectKey);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除OSS中的文件
     * @param path OSS中的存储路径（objectKey）
     * @return true-删除成功或文件不存在，false-删除失败
     */
    @Override
    public boolean delete(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        try {
            // 假设你的域名包含 ".aliyuncs.com/"
            if (path.startsWith("http://") || path.startsWith("https://")) {
                // 获取域名后面的核心路径
                String hostSuffix = ".aliyuncs.com/";
                if (path.contains(hostSuffix)) {
                    path = path.substring(path.indexOf(hostSuffix) + hostSuffix.length());
                } else if (path.contains(properties.getBucketName())) {
                    // 自定义域名情况的处理（如果绑定了自定义域名）
                    path = path.substring(path.indexOf(properties.getBucketName()) + properties.getBucketName().length() + 1);
                }
            }

            // 如果路径开头一不小心带了 "/"，也要去掉
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // 先检查文件是否存在
            if (!exists(path)) {
                // 文件不存在，视为删除成功（幂等性）
                return true;
            }

            // 执行删除操作
            ossClient.deleteObject(properties.getBucketName(), path);

            // 验证删除是否成功
            return !exists(path);
        } catch (Exception e) {
            // 记录日志但不抛出异常，返回false表示删除失败
            System.err.println("删除文件失败: " + path + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 生成不冲突的文件名
     * @param path 文件名
     * @return 生成的文件名
     */
    private String nonePath(String path){
        int i1 = path.lastIndexOf("/");
        int i2 = path.lastIndexOf(".");
        String fileName = path.substring(i1 + 1, i2);
        String doc = path.substring(i1 + 1);
        String ext = path.substring(i2);
        for(int i = 1;i <= 10 ; i++){
            String newPath = path.replace(doc, fileName + "_" + i + ext);
            if(!exists(newPath)){
                return newPath;
            }
        }
        throw new RuntimeException("过多同名文件");
    }
}
