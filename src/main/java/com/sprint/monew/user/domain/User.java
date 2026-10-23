package com.sprint.monew.user.domain;

import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = User.EMAIL_UNIQUE_CONSTRAINT, columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeleteEntity {

    public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String password;

    public User(String email, String nickname, String encodedPassword) {
        this.email = email;
        this.nickname = nickname;
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
