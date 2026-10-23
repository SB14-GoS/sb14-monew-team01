package com.sprint.monew.interest.web;

import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "관심사 관리")
public interface InterestApiDocs {
    @Operation(summary = "관심사 등록")
    ResponseEntity<InterestDto> registerInterest(@Valid @RequestBody InterestRegisterRequest request);
}
