package com.sprint.monew.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserPurgeJobTest {

    @Autowired
    JobOperator jobOperator;

    @Autowired
    Job userPurgeJob;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("탈퇴 후 1일이 지난 사용자만 물리 삭제한다")
    void purgeUsersDeletedMoreThanOneDayAgo() throws Exception {
        Instant now = Instant.now();
        User expired = saveDeletedUser("woody@monew.com", "우디", now.minus(Duration.ofDays(2)));
        User recent = saveDeletedUser("buzz@monew.com", "버즈", now.minus(Duration.ofHours(1)));
        User active = userRepository.save(new User("jessie@monew.com", "제시", "encoded-password"));

        JobExecution execution = jobOperator.start(
                userPurgeJob,
                new JobParametersBuilder()
                        .addLong(UserPurgeJobConfig.REQUESTED_AT, now.toEpochMilli())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(userRepository.existsById(expired.getId())).isFalse();
        assertThat(userRepository.existsById(recent.getId())).isTrue();
        assertThat(userRepository.existsById(active.getId())).isTrue();
    }

    private User saveDeletedUser(String email, String nickname, Instant deletedAt) {
        User user = userRepository.save(new User(email, nickname, "encoded-password"));
        jdbcTemplate.update("update users set deleted_at = ? where id = ?", Timestamp.from(deletedAt), user.getId());
        return user;
    }
}
