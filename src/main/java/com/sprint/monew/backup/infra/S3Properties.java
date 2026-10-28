package com.sprint.monew.backup.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("monew.storage.s3")
public record S3Properties(String bucket, String region) {}
