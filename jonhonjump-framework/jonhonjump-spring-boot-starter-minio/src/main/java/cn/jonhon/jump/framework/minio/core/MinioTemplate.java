package cn.jonhon.jump.framework.minio.core;

import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

/**
 * Common MinIO operations.
 */
@RequiredArgsConstructor
public class MinioTemplate {

    private final MinioClient minioClient;
    private final String bucketName;

    public boolean bucketExists() throws Exception {
        return bucketExists(bucketName);
    }

    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(requireBucketName(bucketName))
                .build());
    }

    public void makeBucketIfAbsent() throws Exception {
        makeBucketIfAbsent(bucketName);
    }

    public void makeBucketIfAbsent(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(requireBucketName(bucketName))
                    .build());
        }
    }

    /**
     * 文件上传
     * @param objectName
     * @param inputStream
     * @param size
     * @param contentType
     * @throws Exception
     */
    public void putObject(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        putObject(bucketName, objectName, inputStream, size, contentType);
    }

    public void putObject(String bucketName, String objectName, InputStream inputStream, long size,
                          String contentType) throws Exception {
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .stream(inputStream, size, -1);
        if (StrUtil.isNotBlank(contentType)) {
            builder.contentType(contentType);
        }
        minioClient.putObject(builder.build());
    }

    /**
     * 上传本地现成文件
     * @param objectName
     * @param fileName
     * @param contentType
     * @throws Exception
     */
    public void uploadObject(String objectName, String fileName, String contentType) throws Exception {
        uploadObject(bucketName, objectName, fileName, contentType);
    }

    public void uploadObject(String bucketName, String objectName, String fileName,
                             String contentType) throws Exception {
        UploadObjectArgs.Builder builder = UploadObjectArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .filename(fileName);
        if (StrUtil.isNotBlank(contentType)) {
            builder.contentType(contentType);
        }
        minioClient.uploadObject(builder.build());
    }

    /**
     * 下载文件
     * @param objectName
     * @return
     * @throws Exception
     */
    public GetObjectResponse getObject(String objectName) throws Exception {
        return getObject(bucketName, objectName);
    }

    public GetObjectResponse getObject(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .build());
    }

    /**
     * 查询文件信息
     * @param objectName
     * @return
     * @throws Exception
     */
    public StatObjectResponse statObject(String objectName) throws Exception {
        return statObject(bucketName, objectName);
    }

    public StatObjectResponse statObject(String bucketName, String objectName) throws Exception {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .build());
    }

    /**
     * 删除 MinIO 中的文件
     * @param objectName
     * @throws Exception
     */
    public void removeObject(String objectName) throws Exception {
        removeObject(bucketName, objectName);
    }

    public void removeObject(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .build());
    }

    /**
     * 生成一个有有效期的临时访问地址
     * @param method
     * @param objectName
     * @param expiry
     * @return
     * @throws Exception
     */
    public String getPresignedObjectUrl(Method method, String objectName, int expiry) throws Exception {
        return getPresignedObjectUrl(bucketName, method, objectName, expiry);
    }

    public String getPresignedObjectUrl(String bucketName, Method method, String objectName,
                                        int expiry) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(requireBucketName(bucketName))
                .object(objectName)
                .method(method)
                .expiry(expiry)
                .build());
    }

    private static String requireBucketName(String bucketName) {
        if (StrUtil.isBlank(bucketName)) {
            throw new IllegalArgumentException("bucketName must not be blank");
        }
        return bucketName;
    }

}
