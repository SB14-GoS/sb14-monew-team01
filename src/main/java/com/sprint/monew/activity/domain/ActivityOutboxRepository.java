package com.sprint.monew.activity.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityOutboxRepository extends JpaRepository<ActivityOutbox, Long> {

    List<ActivityOutbox> findByStatusOrderByIdAsc(ActivityOutboxStatus status, Limit limit);

    long countByStatus(ActivityOutboxStatus status);

    /**
     * 트랜잭션이 끝날 때까지 유지되는 PostgreSQL advisory lock. 잡지 못하면 바로 false를 돌려준다.
     *
     * <p>여러 인스턴스 중 한 곳만 relay를 실행하게 해 이벤트 반영 순서를 지킨다.
     */
    @Query(value = "select pg_try_advisory_xact_lock(:key)", nativeQuery = true)
    boolean tryTransactionLock(@Param("key") long key);

    @Modifying
    @Query("delete from ActivityOutbox o where o.status = :status and o.processedAt < :before")
    int deleteProcessedBefore(@Param("status") ActivityOutboxStatus status, @Param("before") Instant before);
}
