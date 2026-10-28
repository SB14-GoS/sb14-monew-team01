package com.sprint.monew.interest.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // 이미 구독 중인지 확인하여 중복 구독을 방지하기 위한 메서드
    boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.userId = :userId AND s.interest.id = :interestId")
    int deleteByUserIdAndInterestId(@Param("userId") UUID userId, @Param("interestId") UUID interestId);

    // 관심사 삭제 전 연관된 구독 데이터 일괄 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.interest.id = :interestId")
    void deleteByInterestId(@Param("interestId") UUID interestId);

    @Query("SELECT s.interest.id FROM Subscription s WHERE s.userId = :userId AND s.interest.id IN :interestIds")
    Set<UUID> findSubscribedInterestIds(@Param("userId") UUID userId, @Param("interestIds") List<UUID> interestIds);

    // 특정 관심사를 구독 중인 모든 사용자의 ID 조회
    @Query("SELECT s.userId FROM Subscription s WHERE s.interest.id = :interestId")
    List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);

    // 특정 사용자가 구독 중인 관심사 목록을 Interest와 함께 조회
    @Query("SELECT s FROM Subscription s JOIN FETCH s.interest WHERE s.userId = :userId")
    List<Subscription> findAllByUserIdWithInterest(@Param("userId") UUID userId);

    // 사용자 물리 삭제 전 구독자 수를 줄이기 위해, 해당 사용자가 구독 중인 관심사 ID 목록 조회
    @Query("Select s.interest.id FROM Subscription s WHERE s.userId = :userId")
    List<UUID> findInterestIdsByUserId(@Param("userId") UUID userId);

    // 사용자 물리 삭제 시 해당 사용자의 구독 데이터 일괄 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
