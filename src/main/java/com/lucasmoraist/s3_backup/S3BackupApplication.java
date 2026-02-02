package com.lucasmoraist.s3_backup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class S3BackupApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3BackupApplication.class, args);
	}

}
