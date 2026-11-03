package com.sprint.monew.backup.web;

import com.sprint.monew.backup.application.ArticleBackupJobConfig;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleBackupDemoController implements ArticleBackupDemoApiDocs {

    private final JobOperator jobOperator;
    private final Job articleBackupJob;

    @Override
    @PostMapping("/api/dev/articles/backup")
    public ResponseEntity<String> backup(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date)
            throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate(ArticleBackupJobConfig.TARGET_DATE, date)
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobOperator.start(articleBackupJob, parameters);
        return ResponseEntity.ok(execution.getStatus().name());
    }
}
