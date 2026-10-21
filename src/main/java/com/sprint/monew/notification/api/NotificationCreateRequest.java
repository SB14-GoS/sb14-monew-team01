package com.sprint.monew.notification.api;

import com.sprint.monew.notification.domain.ResourceType;
import java.util.UUID;

public record NotificationCreateRequest(UUID userId, String content, ResourceType resourceType, UUID resourceId) {}
