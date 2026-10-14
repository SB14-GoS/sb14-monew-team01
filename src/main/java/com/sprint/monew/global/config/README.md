# `com.sprint.monew.global.config`



## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `JpaConfig` | @Configuration | `@EnableJpaAuditing` |
| `QuerydslConfig` | @Configuration | `JPAQueryFactory` 빈 |
| `MongoConfig` | @Configuration | 필요 시 Mongo 설정 |
| `SwaggerConfig` | @Configuration | OpenAPI 제목·버전·서버 정보 |
| `AsyncConfig / SchedulingConfig` | @Configuration | `@EnableAsync`, `@EnableScheduling` |
| `AwsConfig` | @Configuration | `S3Client` 빈 |

**관련 이슈**: [#6](https://github.com/SB14-GoS/sb14-monew-team01/issues/6) · [#9](https://github.com/SB14-GoS/sb14-monew-team01/issues/9)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
