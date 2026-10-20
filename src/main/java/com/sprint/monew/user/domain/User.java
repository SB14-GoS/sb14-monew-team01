package com.sprint.monew.user.domain;

import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class User extends BaseSoftDeleteEntity {

    private String userNickname;
}
