package com.sprint.monew.interest.web;

import com.sprint.monew.interest.application.InterestService;
import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
