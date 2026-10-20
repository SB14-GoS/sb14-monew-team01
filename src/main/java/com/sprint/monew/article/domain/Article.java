package com.sprint.monew.article.domain;

import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article")
@NoArgsConstructor
public class Article extends BaseSoftDeleteEntity {}
