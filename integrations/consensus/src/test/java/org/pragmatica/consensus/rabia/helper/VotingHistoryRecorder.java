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

package org.pragmatica.consensus.rabia.helper;

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.rabia.Batch;
import org.pragmatica.consensus.rabia.CorrelationId;
import org.pragmatica.consensus.rabia.Phase;
import org.pragmatica.consensus.rabia.StateValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Records protocol events for debugging and analysis.
 */
public final class VotingHistoryRecorder<C extends Command> {

    /// Sealed interface for all history events
    public sealed interface HistoryEvent {
        long timestamp();

        record ProposalEvent(long timestamp, NodeId node, CorrelationId batchId) implements HistoryEvent {}

        record Round1VoteEvent(long timestamp, NodeId node, Phase phase, StateValue value) implements HistoryEvent {}

        record Round2VoteEvent(long timestamp, NodeId node, Phase phase, StateValue value) implements HistoryEvent {}

        record DecisionEvent(long timestamp, NodeId node, Phase phase, StateValue value) implements HistoryEvent {}

        record CoinFlipEvent(long timestamp, Phase phase, StateValue value) implements HistoryEvent {}

        record PhaseStartEvent(long timestamp, NodeId node, Phase phase) implements HistoryEvent {}

        record PhaseCompleteEvent(long timestamp, NodeId node, Phase phase) implements HistoryEvent {}
    }

    private final List<HistoryEvent> events = new ArrayList<>();
    private long eventCounter = 0;

    public void recordProposal(NodeId node, Batch<C> batch) {
        events.add(new HistoryEvent.ProposalEvent(eventCounter++, node, batch.correlationId()));
    }

    public void recordRound1Vote(NodeId node, Phase phase, StateValue value) {
        events.add(new HistoryEvent.Round1VoteEvent(eventCounter++, node, phase, value));
    }

    public void recordRound2Vote(NodeId node, Phase phase, StateValue value) {
        events.add(new HistoryEvent.Round2VoteEvent(eventCounter++, node, phase, value));
    }

    public void recordDecision(NodeId node, Phase phase, StateValue value) {
        events.add(new HistoryEvent.DecisionEvent(eventCounter++, node, phase, value));
    }

    public void recordCoinFlip(Phase phase, StateValue value) {
        events.add(new HistoryEvent.CoinFlipEvent(eventCounter++, phase, value));
    }

    public void recordPhaseStart(NodeId node, Phase phase) {
        events.add(new HistoryEvent.PhaseStartEvent(eventCounter++, node, phase));
    }

    public void recordPhaseComplete(NodeId node, Phase phase) {
        events.add(new HistoryEvent.PhaseCompleteEvent(eventCounter++, node, phase));
    }

    public List<HistoryEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<HistoryEvent.Round1VoteEvent> findRound1VotesForPhase(Phase phase) {
        return events.stream()
                     .filter(e -> e instanceof HistoryEvent.Round1VoteEvent r && r.phase().equals(phase))
                     .map(e -> (HistoryEvent.Round1VoteEvent) e)
                     .toList();
    }

    public List<HistoryEvent.Round2VoteEvent> findRound2VotesForPhase(Phase phase) {
        return events.stream()
                     .filter(e -> e instanceof HistoryEvent.Round2VoteEvent r && r.phase().equals(phase))
                     .map(e -> (HistoryEvent.Round2VoteEvent) e)
                     .toList();
    }

    public List<HistoryEvent.DecisionEvent> findDecisionsForPhase(Phase phase) {
        return events.stream()
                     .filter(e -> e instanceof HistoryEvent.DecisionEvent d && d.phase().equals(phase))
                     .map(e -> (HistoryEvent.DecisionEvent) e)
                     .toList();
    }

    public List<HistoryEvent.Round1VoteEvent> findConflictingRound1Votes(Phase phase) {
        var votes = findRound1VotesForPhase(phase);
        var valuesByNode = votes.stream()
                                .collect(Collectors.groupingBy(HistoryEvent.Round1VoteEvent::node));

        var conflicts = new ArrayList<HistoryEvent.Round1VoteEvent>();
        for (var entry : valuesByNode.entrySet()) {
            var nodeVotes = entry.getValue();
            if (nodeVotes.size() > 1) {
                conflicts.addAll(nodeVotes);
            }
        }
        return conflicts;
    }

    public Map<StateValue, Long> countRound1VotesByValue(Phase phase) {
        return findRound1VotesForPhase(phase).stream()
                                              .collect(Collectors.groupingBy(
                                                  HistoryEvent.Round1VoteEvent::value,
                                                  Collectors.counting()
                                              ));
    }

    public Map<StateValue, Long> countRound2VotesByValue(Phase phase) {
        return findRound2VotesForPhase(phase).stream()
                                              .collect(Collectors.groupingBy(
                                                  HistoryEvent.Round2VoteEvent::value,
                                                  Collectors.counting()
                                              ));
    }

    public String formatHistory() {
        var sb = new StringBuilder("Protocol History:\n");
        for (var event : events) {
            sb.append(String.format("[%04d] %s%n", event.timestamp(), formatEvent(event)));
        }
        return sb.toString();
    }

    private String formatEvent(HistoryEvent event) {
        return switch (event) {
            case HistoryEvent.ProposalEvent e ->
                String.format("PROPOSE: %s proposed batch %s", e.node().id(), e.batchId().id());
            case HistoryEvent.Round1VoteEvent e ->
                String.format("ROUND1: %s voted %s at phase %d", e.node().id(), e.value(), e.phase().value());
            case HistoryEvent.Round2VoteEvent e ->
                String.format("ROUND2: %s voted %s at phase %d", e.node().id(), e.value(), e.phase().value());
            case HistoryEvent.DecisionEvent e ->
                String.format("DECIDE: %s decided %s at phase %d", e.node().id(), e.value(), e.phase().value());
            case HistoryEvent.CoinFlipEvent e ->
                String.format("COIN: phase %d flipped to %s", e.phase().value(), e.value());
            case HistoryEvent.PhaseStartEvent e ->
                String.format("PHASE_START: %s started phase %d", e.node().id(), e.phase().value());
            case HistoryEvent.PhaseCompleteEvent e ->
                String.format("PHASE_COMPLETE: %s completed phase %d", e.node().id(), e.phase().value());
        };
    }

    public String formatPhaseHistory(Phase phase) {
        var sb = new StringBuilder();
        sb.append("Phase ").append(phase.value()).append(" History:\n");

        for (var event : events) {
            boolean include = switch (event) {
                case HistoryEvent.Round1VoteEvent e -> e.phase().equals(phase);
                case HistoryEvent.Round2VoteEvent e -> e.phase().equals(phase);
                case HistoryEvent.DecisionEvent e -> e.phase().equals(phase);
                case HistoryEvent.CoinFlipEvent e -> e.phase().equals(phase);
                case HistoryEvent.PhaseStartEvent e -> e.phase().equals(phase);
                case HistoryEvent.PhaseCompleteEvent e -> e.phase().equals(phase);
                default -> false;
            };
            if (include) {
                sb.append(String.format("  [%04d] %s%n", event.timestamp(), formatEvent(event)));
            }
        }
        return sb.toString();
    }

    public void clear() {
        events.clear();
        eventCounter = 0;
    }

    public long getEventCount() {
        return eventCounter;
    }

    public List<Phase> getAllPhases() {
        return events.stream()
                     .flatMap(e -> switch (e) {
                         case HistoryEvent.Round1VoteEvent r -> java.util.stream.Stream.of(r.phase());
                         case HistoryEvent.Round2VoteEvent r -> java.util.stream.Stream.of(r.phase());
                         case HistoryEvent.DecisionEvent d -> java.util.stream.Stream.of(d.phase());
                         case HistoryEvent.CoinFlipEvent c -> java.util.stream.Stream.of(c.phase());
                         case HistoryEvent.PhaseStartEvent p -> java.util.stream.Stream.of(p.phase());
                         case HistoryEvent.PhaseCompleteEvent p -> java.util.stream.Stream.of(p.phase());
                         default -> java.util.stream.Stream.empty();
                     })
                     .distinct()
                     .sorted()
                     .toList();
    }
}
