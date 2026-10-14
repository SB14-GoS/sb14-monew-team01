package com.sprint.monew.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class IntegrationTestInfraTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MongoTemplate mongoTemplate;

    @Test
    @DisplayName("PostgreSQL 컨테이너에 연결된다")
    void postgresConnected() {
        String version = jdbcTemplate.queryForObject("select version()", String.class);

        assertThat(version).startsWith("PostgreSQL 17");
    }

    @Test
    @DisplayName("MongoDB 컨테이너에 연결된다")
    void mongoConnected() {
        Document result = mongoTemplate.executeCommand("{ ping: 1 }");

        assertThat(result.get("ok")).isEqualTo(1.0);
    }
}
