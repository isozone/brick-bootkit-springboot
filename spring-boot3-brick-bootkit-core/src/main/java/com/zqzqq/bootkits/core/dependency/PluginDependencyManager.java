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


package com.zqzqq.bootkits.core.dependency;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件依赖管理器
 * 负责插件依赖的解析、验证和管理
 */
public class PluginDependencyManager {
    
    private final Map<String, PluginDependency> pluginDependencies;
    private final Map<String, Set<String>> dependencyGraph;
    private final Map<String, Set<String>> reverseDependencyGraph;
    
    public PluginDependencyManager() {
        this.pluginDependencies = new ConcurrentHashMap<>();
        this.dependencyGraph = new ConcurrentHashMap<>();
        this.reverseDependencyGraph = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册插件依赖信息
     */
    public void registerPluginDependency(String pluginId, PluginDependency dependency) {
        pluginDependencies.put(pluginId, dependency);
        updateDependencyGraph(pluginId, dependency);
    }
    
    /**
     * 解析插件依赖
     */
    public PluginDependencyResolution resolveDependencies(String pluginId) {
        if (!pluginDependencies.containsKey(pluginId)) {
            return new PluginDependencyResolution(false, 
                Arrays.asList("插件 " + pluginId + " 的依赖信息未找到"),
                new ArrayList<>(),
                new ArrayList<>());
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        try {
            resolveDependenciesInternal(pluginId, visited, visiting, errors, warnings);
            
            boolean success = errors.isEmpty();
            return new PluginDependencyResolution(success, errors, warnings, 
                new ArrayList<>(visited));
                
        } catch (DependencyCycleException e) {
            errors.add("检测到依赖循环： " + e.getMessage());
            return new PluginDependencyResolution(false, errors, warnings, 
                new ArrayList<>(visited));
        }
    }
    
    /**
     * 验证插件依赖兼容性
     */
    public PluginCompatibilityResult checkCompatibility(String pluginId, 
                                                       Collection<String> otherPlugins) {
        PluginDependency pluginDep = pluginDependencies.get(pluginId);
        if (pluginDep == null) {
            return new PluginCompatibilityResult(false, 
                Arrays.asList("插件 " + pluginId + " 的依赖信息未找到"));
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 检查必需依赖
        for (String required : pluginDep.getRequiredDependencies()) {
            if (!otherPlugins.contains(required)) {
                errors.add("缺少必需依赖： " + required);
            }
        }
        
        // 检查可选依赖
        for (String optional : pluginDep.getOptionalDependencies()) {
            if (otherPlugins.contains(optional)) {
                warnings.add("发现可选依赖： " + optional);
            }
        }
        
        // 检查依赖冲突
        for (String conflict : pluginDep.getConflicts()) {
            if (otherPlugins.contains(conflict)) {
                errors.add("与依赖冲突： " + conflict);
            }
        }
        
        // 检查版本约束
        for (Map.Entry<String, VersionConstraint> entry : 
             pluginDep.getVersionConstraints().entrySet()) {
            String depId = entry.getKey();
            VersionConstraint constraint = entry.getValue();
            
            if (otherPlugins.contains(depId)) {
                PluginDependency dep = pluginDependencies.get(depId);
                if (dep != null && !constraint.isSatisfied(dep.getVersion())) {
                    errors.add("版本约束不满足： " + depId + " 的版本 " + 
                              dep.getVersion() + " 不满足约束 " + constraint);
                }
            }
        }
        
        return new PluginCompatibilityResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * 获取插件的依赖列表
     */
    public Collection<String> getPluginDependencies(String pluginId) {
        Set<String> result = new HashSet<>();
        
        PluginDependency dep = pluginDependencies.get(pluginId);
        if (dep != null) {
            result.addAll(dep.getRequiredDependencies());
            result.addAll(dep.getOptionalDependencies());
        }
        
        return result;
    }
    
    /**
     * 获取依赖指定插件的插件列表
     */
    public Collection<String> getReverseDependencies(String pluginId) {
        return reverseDependencyGraph.getOrDefault(pluginId, new HashSet<>());
    }
    
    /**
     * 检查是否存在依赖循环
     */
    public boolean hasDependencyCycle(String pluginId) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        try {
            hasDependencyCycleInternal(pluginId, visited, visiting);
            return false;
        } catch (DependencyCycleException e) {
            return true;
        }
    }
    
    /**
     * 获取依赖拓扑排序
     */
    public List<String> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        List<String> result = new ArrayList<>();
        
        // 计算入度
        for (String pluginId : pluginDependencies.keySet()) {
            inDegree.put(pluginId, 0);
        }
        
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            for (String dependency : entry.getValue()) {
                inDegree.put(dependency, inDegree.getOrDefault(dependency, 0) + 1);
            }
        }
        
        // 找到入度为0的节点
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        // Kahn算法
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            Set<String> dependencies = dependencyGraph.get(current);
            if (dependencies != null) {
                for (String dep : dependencies) {
                    int newInDegree = inDegree.get(dep) - 1;
                    inDegree.put(dep, newInDegree);
                    
                    if (newInDegree == 0) {
                        queue.offer(dep);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 移除插件依赖信息
     */
    public void removePluginDependency(String pluginId) {
        PluginDependency removed = pluginDependencies.remove(pluginId);
        if (removed != null) {
            removeFromDependencyGraph(pluginId, removed);
        }
    }
    
    /**
     * 清除所有依赖信息
     */
    public void clear() {
        pluginDependencies.clear();
        dependencyGraph.clear();
        reverseDependencyGraph.clear();
    }
    
    /**
     * 获取已注册插件数量
     */
    public int getRegisteredPluginCount() {
        return pluginDependencies.size();
    }
    
    private void updateDependencyGraph(String pluginId, PluginDependency dependency) {
        // 移除旧的依赖关系
        Set<String> oldDependencies = dependencyGraph.remove(pluginId);
        if (oldDependencies != null) {
            for (String depId : oldDependencies) {
                Set<String> reverseDeps = reverseDependencyGraph.get(depId);
                if (reverseDeps != null) {
                    reverseDeps.remove(pluginId);
                }
            }
        }
        
        // 添加新的依赖关系
        Set<String> newDependencies = new HashSet<>();
        newDependencies.addAll(dependency.getRequiredDependencies());
        newDependencies.addAll(dependency.getOptionalDependencies());
        
        dependencyGraph.put(pluginId, newDependencies);
        
        for (String depId : newDependencies) {
            reverseDependencyGraph.computeIfAbsent(depId, k -> new HashSet<>()).add(pluginId);
        }
    }
    
    private void removeFromDependencyGraph(String pluginId, PluginDependency dependency) {
        Set<String> dependencies = new HashSet<>();
        dependencies.addAll(dependency.getRequiredDependencies());
        dependencies.addAll(dependency.getOptionalDependencies());
        
        for (String depId : dependencies) {
            Set<String> reverseDeps = reverseDependencyGraph.get(depId);
            if (reverseDeps != null) {
                reverseDeps.remove(pluginId);
                if (reverseDeps.isEmpty()) {
                    reverseDependencyGraph.remove(depId);
                }
            }
        }
    }
    
    private void resolveDependenciesInternal(String pluginId, Set<String> visited, 
                                           Set<String> visiting, List<String> errors, 
                                           List<String> warnings) throws DependencyCycleException {
        if (visited.contains(pluginId)) {
            return;
        }
        
        if (visiting.contains(pluginId)) {
            throw new DependencyCycleException("在插件 " + pluginId + " 中检测到循环依赖");
        }
        
        visiting.add(pluginId);
        visited.add(pluginId);
        
        PluginDependency dep = pluginDependencies.get(pluginId);
        if (dep != null) {
            // 检查必需依赖
            for (String required : dep.getRequiredDependencies()) {
                if (!pluginDependencies.containsKey(required)) {
                    errors.add("缺少必需依赖： " + required);
                } else {
                    resolveDependenciesInternal(required, visited, visiting, errors, warnings);
                }
            }
            
            // 检查可选依赖
            for (String optional : dep.getOptionalDependencies()) {
                if (pluginDependencies.containsKey(optional)) {
                    resolveDependenciesInternal(optional, visited, visiting, errors, warnings);
                }
            }
            
            // 检查弃用警告
            for (String depId : dep.getRequiredDependencies()) {
                PluginDependency depInfo = pluginDependencies.get(depId);
                if (depInfo != null && depInfo.isDeprecated()) {
                    warnings.add("依赖插件已弃用： " + depId + " - " + depInfo.getDeprecationMessage());
                }
            }
        }
        
        visiting.remove(pluginId);
    }
    
    private void hasDependencyCycleInternal(String pluginId, Set<String> visited, 
                                          Set<String> visiting) throws DependencyCycleException {
        if (visited.contains(pluginId)) {
            return;
        }
        
        if (visiting.contains(pluginId)) {
            throw new DependencyCycleException("在插件 " + pluginId + " 中检测到循环依赖");
        }
        
        visiting.add(pluginId);
        
        Set<String> dependencies = dependencyGraph.get(pluginId);
        if (dependencies != null) {
            for (String depId : dependencies) {
                hasDependencyCycleInternal(depId, visited, visiting);
            }
        }
        
        visiting.remove(pluginId);
        visited.add(pluginId);
    }
    
    /**
     * 依赖循环异常
     */
    public static class DependencyCycleException extends Exception {
        public DependencyCycleException(String message) {
            super(message);
        }
    }
}