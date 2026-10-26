package com.sprint.monew.article.api;

import java.util.UUID;

public record ArticlesCollectedEvent(UUID interestId, String interestName, long articleCount) {}
