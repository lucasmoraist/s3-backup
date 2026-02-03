package com.lucasmoraist.s3_backup.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Log4j2
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public void uploadFile(String localFilePath) {
        File file = new File(localFilePath);

        if (!file.exists()) {
            log.error("Backup file not found - {}", localFilePath);
            return;
        }

        // Gera um nome único para o arquivo no S3 (ex: 2023/10/backup_users_2023-10-27.csv)
        String s3FileName = generateS3Key(file.getName());

        log.debug("Initiating upload of file {} to S3 bucket {} with key {}", localFilePath, bucketName, s3FileName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3FileName)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));
        log.debug("File uploaded successfully to S3: s3://{}/{}", bucketName, s3FileName);
    }

    private String generateS3Key(String fileName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String datePath = dateFormat.format(new Date());
        // timestamp para evitar colisão se rodar 2x no mesmo dia
        long timestamp = System.currentTimeMillis();
        return "backups/" + datePath + "/" + timestamp + "_" + fileName;
    }

}
