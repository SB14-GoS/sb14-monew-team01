package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.application.ActivityOutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ActivityOutboxProperties.class)
public class ActivityOutboxConfig {}
