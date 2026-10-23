package com.sprint.monew.interest.api;

import java.util.List;
import java.util.UUID;

public record InterestKeywordInfo(UUID interestId, String name, List<String> keywords) {}
