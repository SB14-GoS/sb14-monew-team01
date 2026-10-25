package com.sprint.monew.interest.web;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import com.sprint.monew.interest.web.dto.InterestSearchRequest;
import com.sprint.monew.interest.web.dto.InterestUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "관심사 관리")
public interface InterestApiDocs {
    @Operation(summary = "관심사 등록")
    ResponseEntity<InterestDto> registerInterest(InterestRegisterRequest request);

    @Operation(summary = "관심사 정보 수정")
    ResponseEntity<InterestDto> updateInterest(UUID interestId, InterestUpdateRequest request);

    @Operation(summary = "관심사 물리 삭제")
    ResponseEntity<Void> deleteInterest(UUID interestId);

    @Operation(summary = "관심사 목록 조회")
    ResponseEntity<CursorPageResponse<InterestDto>> getInterest(InterestSearchRequest request, UUID userId);
}
