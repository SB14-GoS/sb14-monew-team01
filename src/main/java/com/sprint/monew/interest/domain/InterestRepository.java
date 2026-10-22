package com.sprint.monew.interest.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

    // 모든 관심사와 키워드 목록을 조회하기 위한 메서드
    @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN FETCH i.keywords")
    List<Interest> findAllWithKeywords();

    // 구독자 수 증가를 위한 메서드(DB를 통한 동시성 제어)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :interestId")
    void increaseSubscriberCount(@Param("interestId") UUID interestId);

    // 구독자 수 감소를 위한 메서드(DB를 통한 동시성 제어)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :interestId")
    void decreaseSubscriberCount(@Param("interestId") UUID interestId);
}
