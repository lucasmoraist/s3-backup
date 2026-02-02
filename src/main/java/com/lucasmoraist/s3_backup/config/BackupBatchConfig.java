package com.lucasmoraist.s3_backup.config;

import com.lucasmoraist.s3_backup.model.User;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BackupBatchConfig {

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
                .resource(new FileSystemResource("backup_users.csv")) // Local do arquivo
                .delimited()
                .delimiter(",")
                .names("id", "name", "email") // Campos da sua Entidade Usuario
                .build();
    }

    // 3. STEP: Junta o Reader e o Writer
    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      ItemReader<User> reader,
                      FlatFileItemWriter<User> writer) {
        return new StepBuilder("stepBackup", jobRepository)
                .<User, User>chunk(1000) // 1. Apenas o tamanho do chunk aqui
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    // 4. JOB: A tarefa final que executa o Step
    @Bean
    public Job exportUserJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("exportUserJob", jobRepository)
                .start(step1)
                .build();
    }

}
