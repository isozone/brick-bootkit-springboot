/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication;

import java.util.*;

/**
 * Circular dependency detector for plugins and services.
 * <p>
 * Uses directed graph algorithms to detect circular dependencies.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class CircularDependencyDetector {

    /**
     * Detect circular dependencies in a dependency graph.
     *
     * @param dependencies dependency graph (node → dependent nodes)
     * @return list of cycles (each cycle is a list of nodes)
     */
    public List<List<String>> detectCycles(Map<String, Set<String>> dependencies) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();

        for (String node : dependencies.keySet()) {
            if (!visited.contains(node)) {
                detectCyclesDFS(node, dependencies, visited, recursionStack, path, cycles);
            }
        }

        return cycles;
    }

    /**
     * DFS-based cycle detection.
     */
    private void detectCyclesDFS(
        String node,
        Map<String, Set<String>> dependencies,
        Set<String> visited,
        Set<String> recursionStack,
        Deque<String> path,
        List<List<String>> cycles
    ) {
        visited.add(node);
        recursionStack.add(node);
        path.push(node);

        Set<String> dependents = dependencies.getOrDefault(node, Collections.emptySet());
        for (String dependent : dependents) {
            if (!visited.contains(dependent)) {
                detectCyclesDFS(dependent, dependencies, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(dependent)) {
                // Found a cycle
                List<String> cycle = new ArrayList<>();
                Iterator<String> iterator = path.descendingIterator();
                cycle.add(dependent);
                while (iterator.hasNext()) {
                    String n = iterator.next();
                    cycle.add(n);
                    if (n.equals(dependent)) {
                        break;
                    }
                }
                cycles.add(cycle);
            }
        }

        path.pop();
        recursionStack.remove(node);
    }

    /**
     * Check if adding a dependency would create a cycle.
     *
     * @param fromNode source node
     * @param toNode target node
     * @param existingDependencies existing dependencies
     * @return true if would create a cycle
     */
    public boolean wouldCreateCycle(
        String fromNode,
        String toNode,
        Map<String, Set<String>> existingDependencies
    ) {
        // If toNode already depends on fromNode, adding fromNode -> toNode would create a cycle
        Set<String> toNodeDeps = existingDependencies.getOrDefault(toNode, Collections.emptySet());
        return toNodeDeps.contains(fromNode);
    }

    /**
     * Format cycles for human-readable output.
     *
     * @param cycles list of cycles
     * @return formatted string
     */
    public String formatCycles(List<List<String>> cycles) {
        if (cycles.isEmpty()) {
            return "No circular dependencies detected.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Detected ").append(cycles.size()).append(" circular dependency(ies):\n");

        for (int i = 0; i < cycles.size(); i++) {
            List<String> cycle = cycles.get(i);
            sb.append(String.format("  Cycle %d: ", i + 1));
            sb.append(String.join(" → ", cycle));
            sb.append(" → ").append(cycle.get(0));  // Close the cycle
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Get the set of nodes reachable from a given node.
     *
     * @param startNode starting node
     * @param dependencies dependency graph
     * @return set of reachable nodes
     */
    public Set<String> getReachableNodes(
        String startNode,
        Map<String, Set<String>> dependencies
    ) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            String node = queue.poll();
            if (reachable.contains(node)) {
                continue;
            }
            reachable.add(node);

            Set<String> dependents = dependencies.getOrDefault(node, Collections.emptySet());
            for (String dependent : dependents) {
                if (!reachable.contains(dependent)) {
                    queue.add(dependent);
                }
            }
        }

        return reachable;
    }

    /**
     * Check if two nodes are in the same dependency component.
     *
     * @param node1 first node
     * @param node2 second node
     * @param dependencies dependency graph
     * @return true if they share dependencies
     */
    public boolean areInSameComponent(
        String node1,
        String node2,
        Map<String, Set<String>> dependencies
    ) {
        Set<String> node1Reachable = getReachableNodes(node1, dependencies);
        return node1Reachable.contains(node2);
    }

    /**
     * Simple graph class for internal use.
     */
    private static class Graph<T> {
        private final Map<T, Set<T>> edges = new HashMap<>();

        public void addVertex(T vertex) {
            edges.putIfAbsent(vertex, new HashSet<>());
        }

        public void addEdge(T from, T to) {
            edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        }

        public Set<T> getAdjacent(T vertex) {
            return edges.getOrDefault(vertex, Collections.emptySet());
        }

        public Set<T> getVertices() {
            return edges.keySet();
        }
    }
}
