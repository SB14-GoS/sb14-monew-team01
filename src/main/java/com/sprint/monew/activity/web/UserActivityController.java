package com.sprint.monew.activity.web;

import com.sprint.monew.activity.application.UserActivityQueryService;
import com.sprint.monew.activity.web.dto.UserActivityDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController implements UserActivityApiDocs {

    private final UserActivityQueryService userActivityQueryService;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserActivityDto> getActivity(@PathVariable UUID userId) {
        return ResponseEntity.ok(userActivityQueryService.getActivity(userId));
    }
}
