package com.sprint.monew.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** {@link com.sprint.monew.global.common.BaseEntity}의 생성·수정 시각 자동 기록을 활성화한다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
