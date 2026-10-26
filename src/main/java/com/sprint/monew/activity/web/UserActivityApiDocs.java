package com.sprint.monew.activity.web;

import com.sprint.monew.activity.web.dto.UserActivityDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 활동 내역 관리")
public interface UserActivityApiDocs {
    @Operation(summary = "사용자 활동 내역 조회")
    ResponseEntity<UserActivityDto> getActivity(UUID userId);
}
