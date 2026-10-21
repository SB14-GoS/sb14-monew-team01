package com.sprint.monew.notification.api;

public interface NotificationCommandApi {

    // 새로운 알림을 생성하는 메서드
    void createNotification(NotificationCreateRequest request);

    // 확인 완료한지 1주일이 지난 알림을 일괄 삭제하는 메서드
    void deleteOldConfirmedNotifications();
}
