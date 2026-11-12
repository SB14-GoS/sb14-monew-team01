package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.monew.activity.application.UserActivityReconciler.ChunkResult;
import com.sprint.monew.activity.application.UserActivityReconciler.ReconcileResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class UserActivityReconcileTaskletTest {

    @Mock
    UserActivityReconciler reconciler;

    private UserActivityReconcileTasklet tasklet;
    private final ExecutionContext context = new ExecutionContext();
    private ChunkContext chunkContext;
    private final StepContribution contribution = mock(StepContribution.class);

    @BeforeEach
    void setUp() {
        tasklet = new UserActivityReconcileTasklet(reconciler);
        StepExecution stepExecution = mock(StepExecution.class);
        given(stepExecution.getExecutionContext()).willReturn(context);
        StepContext stepContext = mock(StepContext.class);
        given(stepContext.getStepExecution()).willReturn(stepExecution);
        chunkContext = mock(ChunkContext.class);
        given(chunkContext.getStepContext()).willReturn(stepContext);
    }

    @Test
    @DisplayName("묶음마다 끊어 실행하고 진행 위치를 남긴 뒤, 사용자 보정 → 고아 문서 정리 순서로 끝낸다")
    void runsChunkByChunk() throws Exception {
        UUID first = UUID.randomUUID();
        UUID orphanCursor = UUID.randomUUID();
        given(reconciler.reconcileUsersAfter(UserActivityReconciler.MIN_ID))
                .willReturn(new ChunkResult(first, new ReconcileResult(500, 3, 1, 4, 2, 0)));
        given(reconciler.reconcileUsersAfter(first)).willReturn(new ChunkResult(null, ReconcileResult.empty()));
        given(reconciler.removeOrphansAfter(UserActivityReconciler.MIN_ID))
                .willReturn(new ChunkResult(orphanCursor, new ReconcileResult(0, 0, 0, 0, 0, 2)));
        given(reconciler.removeOrphansAfter(orphanCursor)).willReturn(new ChunkResult(null, ReconcileResult.empty()));

        assertThat(tasklet.execute(contribution, chunkContext)).isEqualTo(RepeatStatus.CONTINUABLE);
        assertThat(context.getString(UserActivityReconcileTasklet.AFTER)).isEqualTo(first.toString());

        assertThat(tasklet.execute(contribution, chunkContext)).isEqualTo(RepeatStatus.CONTINUABLE);
        assertThat(context.getString(UserActivityReconcileTasklet.PHASE))
                .isEqualTo(UserActivityReconcileTasklet.ORPHANS);

        assertThat(tasklet.execute(contribution, chunkContext)).isEqualTo(RepeatStatus.CONTINUABLE);
        assertThat(tasklet.execute(contribution, chunkContext)).isEqualTo(RepeatStatus.FINISHED);

        assertThat(UserActivityReconcileTasklet.totalOf(context)).isEqualTo(new ReconcileResult(500, 3, 1, 4, 2, 2));
        verify(contribution).incrementWriteCount(3);
        verify(contribution).incrementWriteCount(2);
    }

    @Test
    @DisplayName("재시작하면 저장된 위치 다음 묶음부터 이어서 한다")
    void resumesFromSavedPosition() throws Exception {
        UUID saved = UUID.randomUUID();
        context.putString(UserActivityReconcileTasklet.PHASE, UserActivityReconcileTasklet.USERS);
        context.putString(UserActivityReconcileTasklet.AFTER, saved.toString());
        given(reconciler.reconcileUsersAfter(saved)).willReturn(new ChunkResult(null, ReconcileResult.empty()));

        tasklet.execute(contribution, chunkContext);

        verify(reconciler).reconcileUsersAfter(saved);
    }
}
