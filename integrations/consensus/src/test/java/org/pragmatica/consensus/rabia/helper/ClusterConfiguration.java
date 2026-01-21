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

import org.pragmatica.consensus.NodeId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Configuration record for cluster testing.
 * For n=2f+1 nodes: quorum = f+1 (majority), fPlusOne = f+1
 */
public record ClusterConfiguration(
    int clusterSize,
    int quorumSize,
    int fPlusOne,
    int maxFailures,
    int superMajoritySize,
    List<NodeId> nodeIds
) {
    public static ClusterConfiguration threeNodes() {
        return of(3);
    }

    public static ClusterConfiguration fiveNodes() {
        return of(5);
    }

    public static ClusterConfiguration sevenNodes() {
        return of(7);
    }

    public static ClusterConfiguration of(int n) {
        int f = (n - 1) / 2;
        var nodeIds = IntStream.rangeClosed(1, n)
                               .mapToObj(i -> NodeId.nodeId("node-" + i).unwrap())
                               .toList();
        return new ClusterConfiguration(n, f + 1, f + 1, f, n - f, nodeIds);
    }

    /// Return all possible majority quorums (combinations of quorumSize nodes)
    public List<Set<NodeId>> allQuorums() {
        return combinations(nodeIds, quorumSize);
    }

    /// Return all f+1 subsets
    public List<Set<NodeId>> allFPlusOneSets() {
        return combinations(nodeIds, fPlusOne);
    }

    /// Return pairs of quorums (for intersection tests)
    public List<List<Set<NodeId>>> quorumPairs() {
        var quorums = allQuorums();
        var pairs = new ArrayList<List<Set<NodeId>>>();
        for (int i = 0; i < quorums.size(); i++) {
            for (int j = i; j < quorums.size(); j++) {
                pairs.add(List.of(quorums.get(i), quorums.get(j)));
            }
        }
        return pairs;
    }

    private static <T> List<Set<T>> combinations(List<T> items, int k) {
        var result = new ArrayList<Set<T>>();
        combinationsHelper(items, k, 0, new HashSet<>(), result);
        return result;
    }

    private static <T> void combinationsHelper(List<T> items, int k, int start, Set<T> current, List<Set<T>> result) {
        if (current.size() == k) {
            result.add(new HashSet<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsHelper(items, k, i + 1, current, result);
            current.remove(items.get(i));
        }
    }
}
