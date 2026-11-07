package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sprint.monew.article.api.ArticlesCollectedEvent;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleInterestIdPair;
import com.sprint.monew.article.domain.repository.ArticleInterestRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.article.infra.source.NaverNewsClient;
import com.sprint.monew.article.infra.source.RssNewsClient;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCollectServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    ArticleInterestRepository articleInterestRepository;

    @Mock
    NaverNewsClient naverNewsClient;

    @Mock
    RssNewsClient chosunNewsClient;

    @Mock
    RssNewsClient yeonhapNewsClient;

    @Mock
    RssNewsClient hankyungNewsClient;

    @Mock
    InterestQueryApi interestQueryApi;

    @Mock
    ApplicationEventPublisher eventPublisher;

    private final UUID interestId = UUID.randomUUID();

    @Test
    void logsNaverFailureSummaryAndRssExceptionCauses() {
        String responseBody = "<rss><item>출력하면 안 되는 기사 원문</item></rss>";
        RuntimeException failure = new IllegalStateException(responseBody, new IllegalArgumentException("연결 시간 초과"));
        Set<String> keywords = Set.of("경제");
        given(interestQueryApi.getAllKeywords())
                .willReturn(List.of(new InterestKeywordInfo(interestId, "경제", List.of("경제"))));
        given(naverNewsClient.fetch(List.of("경제"), Instant.MIN)).willThrow(failure);
        given(chosunNewsClient.fetch(keywords)).willThrow(failure);
        given(yeonhapNewsClient.fetch(keywords)).willThrow(failure);
        given(hankyungNewsClient.fetch(keywords)).willThrow(failure);

        Logger logger = (Logger) LoggerFactory.getLogger(ArticleCollectService.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            service().save();

            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .hasSize(4)
                    .allSatisfy(event -> {
                        assertThat(event.getFormattedMessage())
                                .contains("수집 실패 - error: IllegalStateException")
                                .doesNotContain(responseBody, "\n", "\r");
                        if (event.getFormattedMessage().startsWith("네이버")) {
                            assertThat(event.getThrowableProxy()).isNull();
                        } else {
                            assertThat(event.getThrowableProxy()).isNotNull();
                            assertThat(event.getThrowableProxy().getClassName())
                                    .isEqualTo(failure.getClass().getName());
                            assertThat(event.getThrowableProxy().getMessage()).isEqualTo(responseBody);
                            assertThat(event.getThrowableProxy().getCause().getMessage())
                                    .isEqualTo("연결 시간 초과");
                        }
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void collectsNaverPerKeywordAndSavesAllSources(boolean hasSavedNaverArticles) {
        List<RawArticle> naverArticles = articles(ArticleSource.NAVER, 2);
        RawArticle latestArticle = naverArticles.get(0);
        given(articleRepository.findLatestNaverArticle())
                .willReturn(hasSavedNaverArticles ? Optional.of(existingArticle(latestArticle)) : Optional.empty());
        Instant latestNaverPublishedAt = hasSavedNaverArticles ? latestArticle.publishDate() : Instant.MIN;
        Set<String> keywords = Set.of("경제", "기사");
        given(interestQueryApi.getAllKeywords())
                .willReturn(List.of(new InterestKeywordInfo(interestId, "경제", List.of("경제", "기사"))));
        given(naverNewsClient.fetch(List.of("경제"), latestNaverPublishedAt)).willReturn(List.of(naverArticles.get(0)));
        given(naverNewsClient.fetch(List.of("기사"), latestNaverPublishedAt)).willReturn(List.of(naverArticles.get(1)));
        given(chosunNewsClient.fetch(keywords)).willReturn(articles(ArticleSource.CHOSUN, 1));
        given(yeonhapNewsClient.fetch(keywords)).willReturn(articles(ArticleSource.YEONHAP, 3));
        given(hankyungNewsClient.fetch(keywords)).willReturn(articles(ArticleSource.HANKYUNG, 2));
        service().save();

        verify(articleRepository).findLatestNaverArticle();
        verify(naverNewsClient).fetch(List.of("경제"), latestNaverPublishedAt);
        verify(naverNewsClient).fetch(List.of("기사"), latestNaverPublishedAt);
        verify(chosunNewsClient).fetch(keywords);
        verify(yeonhapNewsClient).fetch(keywords);
        verify(hankyungNewsClient).fetch(keywords);
        ArgumentCaptor<Iterable<Article>> savedArticles = ArgumentCaptor.captor();
        verify(articleRepository).saveAll(savedArticles.capture());
        assertThat(savedArticles.getValue()).hasSize(8);
        ArgumentCaptor<Iterable<ArticleInterest>> savedMappings = ArgumentCaptor.captor();
        verify(articleInterestRepository).saveAll(savedMappings.capture());
        assertThat(savedMappings.getValue()).hasSize(8);
        verify(eventPublisher).publishEvent(new ArticlesCollectedEvent(interestId, "경제", 8));
    }

    @ParameterizedTest
    @CsvSource({"false,false,1", "true,false,0", "false,true,0"})
    void handlesExistingArticleWithoutNotification(boolean mappingExists, boolean deleted, int expectedMappings) {
        RawArticle raw = articles(ArticleSource.NAVER, 1).get(0);
        Article existing = existingArticle(raw);
        if (deleted) {
            existing.softDelete();
        } else {
            given(articleInterestRepository.findInterestIdPairsByArticleIds(List.of(existing.getId())))
                    .willReturn(
                            mappingExists
                                    ? List.of(new ArticleInterestIdPair(existing.getId(), interestId))
                                    : List.of());
        }
        given(articleRepository.findAllBySourceUrlIn(List.of(raw.sourceUrl()))).willReturn(List.of(existing));

        collect(List.of(raw));

        ArgumentCaptor<Iterable<Article>> savedArticles = ArgumentCaptor.captor();
        verify(articleRepository).saveAll(savedArticles.capture());
        assertThat(savedArticles.getValue()).isEmpty();
        ArgumentCaptor<Iterable<ArticleInterest>> savedMappings = ArgumentCaptor.captor();
        verify(articleInterestRepository).saveAll(savedMappings.capture());
        assertThat(savedMappings.getValue()).hasSize(expectedMappings);
        for (ArticleInterest mapping : savedMappings.getValue()) {
            assertThat(mapping.getArticle()).isSameAs(existing);
            assertThat(mapping.getInterestId()).isEqualTo(interestId);
        }
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void countsOnlyNewArticlesEvenWhenExistingArticlesReceiveMappings() {
        List<RawArticle> collected = articles(ArticleSource.NAVER, 2);
        Article existing = existingArticle(collected.get(0));
        given(articleRepository.findAllBySourceUrlIn(
                        collected.stream().map(RawArticle::sourceUrl).toList()))
                .willReturn(List.of(existing));

        collect(collected);

        ArgumentCaptor<Iterable<Article>> savedArticles = ArgumentCaptor.captor();
        verify(articleRepository).saveAll(savedArticles.capture());
        assertThat(savedArticles.getValue()).hasSize(1);
        ArgumentCaptor<Iterable<ArticleInterest>> savedMappings = ArgumentCaptor.captor();
        verify(articleInterestRepository).saveAll(savedMappings.capture());
        assertThat(savedMappings.getValue()).hasSize(2);
        verify(eventPublisher).publishEvent(new ArticlesCollectedEvent(interestId, "경제", 1));
    }

    @Test
    void countsDuplicateNewArticleAndMultipleMatchingKeywordsOnlyOnce() {
        RawArticle raw = articles(ArticleSource.NAVER, 1).get(0);

        collect(List.of(raw, raw));

        ArgumentCaptor<Iterable<ArticleInterest>> savedMappings = ArgumentCaptor.captor();
        verify(articleInterestRepository).saveAll(savedMappings.capture());
        assertThat(savedMappings.getValue()).hasSize(1);
        verify(eventPublisher).publishEvent(new ArticlesCollectedEvent(interestId, "경제", 1));
    }

    private void collect(List<RawArticle> articles) {
        given(interestQueryApi.getAllKeywords())
                .willReturn(List.of(new InterestKeywordInfo(interestId, "경제", List.of("경제", "기사"))));
        given(naverNewsClient.fetch(List.of("경제"), Instant.MIN)).willReturn(articles);
        given(naverNewsClient.fetch(List.of("기사"), Instant.MIN)).willReturn(List.of());
        service().save();
    }

    private static Article existingArticle(RawArticle raw) {
        Article article =
                Article.create(raw.title(), raw.publishDate(), raw.summary(), raw.articleSource(), raw.sourceUrl());
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        return article;
    }

    private ArticleCollectService service() {
        return new ArticleCollectService(
                naverNewsClient,
                chosunNewsClient,
                yeonhapNewsClient,
                hankyungNewsClient,
                interestQueryApi,
                new ArticleMappingService(articleRepository, articleInterestRepository, eventPublisher),
                articleRepository);
    }

    private static List<RawArticle> articles(ArticleSource source, int count) {
        List<RawArticle> articles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            articles.add(new RawArticle(
                    source,
                    "https://publisher.example/" + source + "/" + i,
                    "경제 기사 " + i,
                    Instant.parse("2026-11-01T00:00:00Z"),
                    "경제 기사 요약"));
        }
        return articles;
    }
}
