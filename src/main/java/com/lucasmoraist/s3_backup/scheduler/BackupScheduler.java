package com.lucasmoraist.s3_backup.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final JobOperator jobOperator;
    private final Job exportUserJob;

    @Scheduled(cron = "#{@backupCronService.getCron()}")
    public void runBackupJob() {
        try {
            log.info("Iniciando o Job de Backup...");

            /*
             * O Spring Batch não roda o mesmo job duas vezes com os mesmos parâmetros.
             * Por isso o System.currentTimeMillis como parâmetro para torná-lo único
             * a cada execução.
             */
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobOperator.start(exportUserJob, jobParameters);
        } catch (Exception e) {
            log.error("Erro ao executar o Job de Backup: ", e);
        }
    }

}
