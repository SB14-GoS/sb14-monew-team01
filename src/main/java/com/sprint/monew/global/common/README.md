# `com.sprint.monew.global.common`



## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `BaseEntity` | @MappedSuperclass | id(UUID), createdAt, updatedAt (Auditing) |
| `BaseSoftDeleteEntity` | @MappedSuperclass | BaseEntity + deletedAt, `softDelete()`, `isDeleted()` |
| `CursorPageResponse<T>` | record | content, nextCursor, nextAfter, size, totalElements, hasNext |
| `CursorRequest` | record | cursor, after, limit (+ 기본값·최대값 검증) |
| `SortDirection` | enum | ASC, DESC |

**관련 이슈**: [#5](https://github.com/SB14-GoS/sb14-monew-team01/issues/5) · [#6](https://github.com/SB14-GoS/sb14-monew-team01/issues/6)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
