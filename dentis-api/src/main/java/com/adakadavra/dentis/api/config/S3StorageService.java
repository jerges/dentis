package com.adakadavra.dentis.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3StorageService {

    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;

    public S3StorageService(
            @Value("${dentis.s3.bucket:}") String bucket,
            @Value("${AWS_REGION:us-east-1}") String region) {
        this.bucket = bucket;
        Region awsRegion = Region.of(region);
        this.s3 = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Builds the S3 key with tenant isolation: {clinicId}/{patientId}/{uuid}/{fileName}
     */
    public String buildKey(UUID clinicId, UUID patientId, String fileName) {
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return clinicId + "/" + patientId + "/" + UUID.randomUUID() + "/" + sanitized;
    }

    /**
     * Returns a presigned PUT URL valid for 15 minutes for direct browser → S3 upload.
     */
    public String presignedUploadUrl(String key, String contentType) {
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(por)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignReq);
        return presigned.url().toString();
    }

    /**
     * Returns a presigned GET URL valid for 60 minutes for secure viewing.
     */
    public String presignedDownloadUrl(String key) {
        GetObjectRequest gor = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(gor)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }

    /**
     * Deletes an object from S3.
     */
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public byte[] getObjectBytes(String key) {
        GetObjectRequest gor = GetObjectRequest.builder().bucket(bucket).key(key).build();
        try (InputStream is = s3.getObject(gor)) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download S3 object: " + key, e);
        }
    }

    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank();
    }
}
