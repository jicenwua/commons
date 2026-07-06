package com.xcz.commons.oss.listener;

import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.xcz.commons.oss.service.UploadService;

/**
 * OSS文件上传进度监听器
 * 用于监听文件上传进度并回调通知
 */
public class PutObjectProgressListener implements ProgressListener {

    private long bytesWritten = 0;
    private long totalBytes = -1;
    private boolean succeed = false;
    private final UploadService.ProgressCallback progressCallback;

    /**
     * 构造函数
     * @param progressCallback 进度回调函数
     */
    public PutObjectProgressListener(UploadService.ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        long bytes = progressEvent.getBytes();
        ProgressEventType eventType = progressEvent.getEventType();

        switch (eventType) {
            case TRANSFER_STARTED_EVENT:
                // 上传开始
                break;
            case REQUEST_CONTENT_LENGTH_EVENT:
                // 获取文件总大小
                this.totalBytes = bytes;
                break;
            case REQUEST_BYTE_TRANSFER_EVENT:
                // 数据传输中，累加已上传字节数
                this.bytesWritten += bytes;
                if (this.totalBytes != -1 && progressCallback != null) {
                    // 回调通知进度
                    progressCallback.onProgress(this.bytesWritten, this.totalBytes);
                }
                break;
            case TRANSFER_COMPLETED_EVENT:
                // 上传完成
                this.succeed = true;
                break;
            case TRANSFER_FAILED_EVENT:
                // 上传失败
                break;
            default:
                break;
        }
    }

    /**
     * 判断上传是否成功
     * @return true-成功，false-失败
     */
    public boolean isSucceed() {
        return succeed;
    }

    /**
     * 获取已上传字节数
     * @return 已上传字节数
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * 获取文件总字节数
     * @return 文件总字节数
     */
    public long getTotalBytes() {
        return totalBytes;
    }
}
