package com.sprint.monew.activity.domain;

public enum ActivityOutboxStatus {
    /** 아직 활동 내역에 반영하지 않음 */
    PENDING,
    /** 반영 완료 */
    DONE,
    /** 반복 실패로 포기함 (정합성 보정 배치가 메운다) */
    DEAD
}
