package com.lucasmoraist.s3_backup.config;

import com.lucasmoraist.s3_backup.model.User;
import com.lucasmoraist.s3_backup.service.S3Service;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BackupBatchConfig {

    private static final String BACKUP_FILE_NAME = "backup_users.csv";

    // 1. READER: Lê do banco usando JPA, mas paginado (seguro para memória)
    @Bean
    public JpaPagingItemReader<User> reader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<User>()
                .name("userItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM t_user u ORDER BY u.id ASC")
                .pageSize(1000) // Lê de 1000 em 1000 registros
                .build();
    }

    // 2. WRITER: Escreve em um arquivo CSV
    @Bean
    public FlatFileItemWriter<User> writer() {
        return new FlatFileItemWriterBuilder<User>()
                .name("userItemWriter")
                .resource(new FileSystemResource(BACKUP_FILE_NAME)) // Local do arquivo
                .delimited()
                .delimiter(",")
                .names("id", "name", "email") // Campos da sua Entidade Usuario
                .build();
    }

    // 3. STEP 1: Gera o CSV
    @Bean
    public Step stepGenerateCsv(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                ItemReader<User> reader,
                                FlatFileItemWriter<User> writer) {
        return new StepBuilder("stepGenerateCsv", jobRepository)
                .<User, User>chunk(1000)
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    // 4. STEP 2: Faz Upload para o S3
    @Bean
    public Step stepUploadS3(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             S3Service s3Service) {

        // Define o que o passo faz: Chama o serviço de upload
        Tasklet uploadTasklet = (contribution, chunkContext) -> {
            s3Service.uploadFile(BACKUP_FILE_NAME);
            return RepeatStatus.FINISHED; // Diz que acabou
        };

        return new StepBuilder("stepUploadS3", jobRepository)
                .tasklet(uploadTasklet, transactionManager)
                .build();
    }

    // 5. JOB: Conecta os passos (Gera CSV -> Upload S3)
    @Bean
    public Job exportUserJob(JobRepository jobRepository, Step stepGenerateCsv, Step stepUploadS3) {
        return new JobBuilder("exportUserJob", jobRepository)
                .start(stepGenerateCsv) // Primeiro gera o arquivo
                .next(stepUploadS3)     // Depois envia para o S3
                .build();
    }

}
