package com.mogu.data.common.storage;

import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MinIO对象存储服务
 * 用于存储模型文件、数据集等
 */
@Service
@RequiredArgsConstructor
public class MinioService {

    private static final Logger log = Logger.getLogger(MinioService.class);

    @Autowired
    private MinioClient minioClient;

    /**
     * 检查桶是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("检查桶是否存在失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建桶
     */
    public void createBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建桶成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("创建桶失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建桶失败", e);
        }
    }

    /**
     * 上传文件
     */
    public void uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            createBucket(bucketName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            log.info("上传文件成功: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 上传MultipartFile
     */
    public void uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            uploadFile(bucketName, objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String bucketName, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("下载文件失败", e);
        }
    }

    /**
     * 检查对象是否存在
     */
    public boolean objectExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (io.minio.errors.ErrorResponseException e) {
            // NoSuchKey 表示对象不存在
            return false;
        } catch (Exception e) {
            log.error("检查对象是否存在失败: {}", e.getMessage(), e);
            throw new RuntimeException("检查对象是否存在失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            log.info("删除文件成功: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    /**
     * 列出所有桶
     */
    public List<String> listBuckets() {
        try {
            return minioClient.listBuckets().stream()
                    .map(Bucket::name)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("列出桶失败: {}", e.getMessage(), e);
            throw new RuntimeException("列出桶失败", e);
        }
    }

    /**
     * 列出桶中的所有对象
     */
    public List<String> listObjects(String bucketName) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .build());

            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : results) {
                try {
                    objectNames.add(result.get().objectName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return objectNames;
        } catch (Exception e) {
            log.error("列出对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("列出对象失败", e);
        }
    }

    /**
     * 获取文件URL
     */
    public String getPresignedUrl(String bucketName, String objectName, int expires) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expires)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取预签名URL失败", e);
        }
    }
}