package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.exception.UserActivityNotFoundException;
import com.sprint.monew.activity.web.dto.UserActivityDto;
import com.sprint.monew.user.api.UserQueryApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityQueryService {

    private final UserActivityRepository userActivityRepository;
    private final UserQueryApi userQueryApi;

    public UserActivityDto getActivity(UUID userId) {
        if (!userQueryApi.exists(userId)) {
            throw new UserActivityNotFoundException(userId);
        }
        UserActivityDocument document =
                userActivityRepository.findById(userId).orElseThrow(() -> new UserActivityNotFoundException(userId));
        return UserActivityDto.from(document);
    }
}
