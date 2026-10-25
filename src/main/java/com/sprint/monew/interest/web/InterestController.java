package com.sprint.monew.interest.web;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.web.RequestUserId;
import com.sprint.monew.interest.application.InterestService;
import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import com.sprint.monew.interest.web.dto.InterestSearchRequest;
import com.sprint.monew.interest.web.dto.InterestUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController implements InterestApiDocs {

    private final InterestService interestService;

    @Override
    @PostMapping
    public ResponseEntity<InterestDto> registerInterest(@Valid @RequestBody InterestRegisterRequest request) {
        InterestDto response = interestService.registerInterest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PatchMapping("/{interestId}")
    public ResponseEntity<InterestDto> updateInterest(
            @PathVariable UUID interestId, @Valid @RequestBody InterestUpdateRequest request) {
        return ResponseEntity.ok(interestService.updateInterest(interestId, request));
    }

    @Override
    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> deleteInterest(@PathVariable UUID interestId) {
        interestService.deleteInterest(interestId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<InterestDto>> getInterest(
            @Valid @ModelAttribute InterestSearchRequest request, @RequestUserId UUID userId) {
        return ResponseEntity.ok(interestService.getInterests(request, userId));
    }
}
