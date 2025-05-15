package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public interface RabiaPersistence<C extends Command> {
    Result<Unit> save(StateMachine<C> stateMachine, Phase lastCommittedPhase, Collection<Batch<C>> pendingBatches);

    Option<SavedState<C>> load();

    static <C extends Command> RabiaPersistence<C> inMemory() {
        record inMemory<C extends Command>(AtomicReference<SavedState<C>> state) implements RabiaPersistence<C> {

            @Override
            public Result<Unit> save(StateMachine<C> stateMachine,
                                     Phase lastCommittedPhase,
                                     Collection<Batch<C>> pendingBatches) {
                return stateMachine.makeSnapshot()
                                   .map(snapshot -> SavedState.savedState(snapshot,
                                                                          lastCommittedPhase,
                                                                          List.copyOf(pendingBatches)))
                                   .onSuccess(state::set)
                                   .onFailure(_ -> state.set(null))
                                   .map(_ -> Unit.unit());
            }

            @Override
            public Option<SavedState<C>> load() {
                return Option.option(state().get());
            }
        }

        return new inMemory<>(new AtomicReference<>());
    }

    interface SavedState<C extends Command> {
        byte[] snapshot();

        Phase lastCommittedPhase();

        List<Batch<C>> pendingBatches();

        static <C extends Command> SavedState<C> savedState(byte[] snapshot,
                                                            Phase lastCommittedPhase,
                                                            Collection<Batch<C>> pendingBatches) {
            record savedState<C extends Command>(byte[] snapshot, Phase lastCommittedPhase,
                                                 List<Batch<C>> pendingBatches) implements SavedState<C> {
                @Override
                public boolean equals(Object o) {
                    if (!(o instanceof savedState<?>(byte[] snapshot1, Phase committedPhase, List<?> batches))) {
                        return false;
                    }

                    return Objects.deepEquals(snapshot(), snapshot1)
                            && Objects.equals(lastCommittedPhase(), committedPhase)
                            && Objects.equals(pendingBatches(), batches);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(Arrays.hashCode(snapshot()), lastCommittedPhase(), pendingBatches());
                }
            }

            return new savedState<>(snapshot, lastCommittedPhase, List.copyOf(pendingBatches));
        }

        static <C extends Command> SavedState<C> empty() {
            return savedState(new byte[0], Phase.ZERO, List.of());
        }
    }
}
