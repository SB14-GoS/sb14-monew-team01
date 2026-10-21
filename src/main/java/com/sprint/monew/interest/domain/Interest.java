package com.sprint.monew.interest.domain;

import com.sprint.monew.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Entity
@Table(name = "interests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Interest extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @Column(name = "subscriber_count", nullable = false)
    long subscriberCount = 0L;

    @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
    List<InterestKeyword> keywords = new ArrayList<>();

    public Interest(String name, List<String> keywords) {
        this.name = name;
        this.subscriberCount = 0L;
        updateKeywords(keywords);
    }

    public void updateKeywords(List<String> newKeywords) {
        this.keywords.clear();
        if (newKeywords != null) {
            newKeywords.stream().distinct().forEach(keyword -> this.keywords.add(new InterestKeyword(this, keyword)));
        }
    }

    public void increaseSubscriberCount() {
        this.subscriberCount++;
    }

    public void decreaseSubscriberCount() {
        if (this.subscriberCount > 0) {
            this.subscriberCount--;
        }
    }

    public List<String> getKeywordStrings() {
        return this.keywords.stream().map(InterestKeyword::getKeyword).toList();
    }
}
