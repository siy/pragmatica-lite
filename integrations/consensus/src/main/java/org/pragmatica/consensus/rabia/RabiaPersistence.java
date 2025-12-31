/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.consensus.rabia;

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Persistence interface for Rabia consensus state.
 */
public interface RabiaPersistence<C extends Command> {
    /**
     * Save the current state.
     */
    Result<Unit> save(StateMachine<C> stateMachine, Phase lastCommittedPhase, Collection<Batch<C>> pendingBatches);

    /**
     * Load the persisted state.
     */
    Option<SavedState<C>> load();

    /**
     * Create an in-memory persistence implementation (for testing or single-session use).
     */
    static <C extends Command> RabiaPersistence<C> inMemory() {
        record inMemory <C extends Command>(AtomicReference<SavedState<C>> state) implements RabiaPersistence<C> {
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
                return Option.option(state()
                                          .get());
            }
        }
        return new inMemory <>(new AtomicReference<>());
    }

    /**
     * Saved consensus state.
     */
    interface SavedState<C extends Command> {
        byte[] snapshot();

        Phase lastCommittedPhase();

        List<Batch<C>> pendingBatches();

        static <C extends Command> SavedState<C> savedState(byte[] snapshot,
                                                            Phase lastCommittedPhase,
                                                            Collection<Batch<C>> pendingBatches) {
            record savedState <C extends Command>(byte[] snapshot,
                                                  Phase lastCommittedPhase,
                                                  List<Batch<C>> pendingBatches) implements SavedState<C> {
                @Override
                public boolean equals(Object o) {
                    if (! (o instanceof savedState < ? >( byte[] snapshot1, Phase committedPhase, List< ? > batches))) {
                        return false;
                    }
                    return Objects.deepEquals(snapshot(), snapshot1) &&
                    Objects.equals(lastCommittedPhase(), committedPhase) &&
                    Objects.equals(pendingBatches(), batches);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(Arrays.hashCode(snapshot()), lastCommittedPhase(), pendingBatches());
                }
            }
            return new savedState <>(snapshot, lastCommittedPhase, List.copyOf(pendingBatches));
        }

        static <C extends Command> SavedState<C> empty() {
            return savedState(new byte[0], Phase.ZERO, List.of());
        }
    }
}
