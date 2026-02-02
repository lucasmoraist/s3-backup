package com.lucasmoraist.s3_backup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("backupCronService")
public class BackupCronService {

    @Value("${backup.cron}")
    private String cronExpression;

    public String getCron() {
        String input = cronExpression.trim().toLowerCase();

        return switch (input) {
            case "minute" -> "0 * * * * *";
            case "daily"  -> "0 0 0 * * *";
            case "weekly" -> "0 0 0 * * SUN";
            case "annual" -> "0 0 0 1 1 *";
            default -> "0 0 0 1 * *";
        };
    }

}
