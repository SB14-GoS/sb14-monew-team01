package com.sprint.monew.interest.domain;

import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;

public record InterestSearchCursor(String cursor, Instant after, int limit, String orderBy, SortDirection direction) {}
