package com.sprint.monew.activity.application;

import com.sprint.monew.activity.application.UserActivityReconciler.ChunkResult;
import com.sprint.monew.activity.application.UserActivityReconciler.ReconcileResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 정합성 보정을 묶음 하나씩 실행한다. 호출마다 트랜잭션이 끝나고 진행 위치가 ExecutionContext에 저장된다.
 *
 * <p>전체를 한 번에 돌리면 tasklet 트랜잭션 하나가 보정 내내 DB 연결을 붙잡는다. 묶음마다 CONTINUABLE을 돌려주면 그 사이 커밋되고, 실패 후
 * 재시작하면 마지막으로 끝낸 묶음 다음부터 이어서 한다. 순서는 사용자 보정 → 고아 문서 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityReconcileTasklet implements Tasklet {

    static final String PHASE = "reconcile.phase";
    static final String AFTER = "reconcile.after";
    static final String TOTAL = "reconcile.total.";
    static final String USERS = "USERS";
    static final String ORPHANS = "ORPHANS";

    private final UserActivityReconciler reconciler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ExecutionContext context =
                chunkContext.getStepContext().getStepExecution().getExecutionContext();
        String phase = context.getString(PHASE, USERS);
        UUID after = UUID.fromString(context.getString(AFTER, UserActivityReconciler.MIN_ID.toString()));

        ChunkResult chunk =
                USERS.equals(phase) ? reconciler.reconcileUsersAfter(after) : reconciler.removeOrphansAfter(after);
        ReconcileResult total = accumulate(context, chunk.result());
        contribution.incrementWriteCount(
                chunk.result().repaired() + chunk.result().orphansRemoved());

        if (chunk.hasNext()) {
            context.putString(PHASE, phase);
            context.putString(AFTER, chunk.nextAfter().toString());
            return RepeatStatus.CONTINUABLE;
        }
        if (USERS.equals(phase)) {
            context.putString(PHASE, ORPHANS);
            context.putString(AFTER, UserActivityReconciler.MIN_ID.toString());
            return RepeatStatus.CONTINUABLE;
        }
        log.info("활동 내역 정합성 보정 완료 - {}", total);
        return RepeatStatus.FINISHED;
    }

    private static ReconcileResult accumulate(ExecutionContext context, ReconcileResult chunk) {
        ReconcileResult total = totalOf(context).plus(chunk);
        context.putInt(TOTAL + "scanned", total.scanned());
        context.putInt(TOTAL + "repaired", total.repaired());
        context.putInt(TOTAL + "created", total.created());
        context.putInt(TOTAL + "itemsAdded", total.itemsAdded());
        context.putInt(TOTAL + "itemsRemoved", total.itemsRemoved());
        context.putInt(TOTAL + "orphansRemoved", total.orphansRemoved());
        return total;
    }

    static ReconcileResult totalOf(ExecutionContext context) {
        return new ReconcileResult(
                context.getInt(TOTAL + "scanned", 0),
                context.getInt(TOTAL + "repaired", 0),
                context.getInt(TOTAL + "created", 0),
                context.getInt(TOTAL + "itemsAdded", 0),
                context.getInt(TOTAL + "itemsRemoved", 0),
                context.getInt(TOTAL + "orphansRemoved", 0));
    }
}
