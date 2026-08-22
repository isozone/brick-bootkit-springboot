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

/**
 * Registry statistics.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class RegistryStatistics {

    private final int totalServices;
    private final int totalPlugins;
    private final int totalInterfaces;
    private final int totalDependencies;

    public RegistryStatistics(
        int totalServices,
        int totalPlugins,
        int totalInterfaces,
        int totalDependencies
    ) {
        this.totalServices = totalServices;
        this.totalPlugins = totalPlugins;
        this.totalInterfaces = totalInterfaces;
        this.totalDependencies = totalDependencies;
    }

    public int getTotalServices() {
        return totalServices;
    }

    public int getTotalPlugins() {
        return totalPlugins;
    }

    public int getTotalInterfaces() {
        return totalInterfaces;
    }

    public int getTotalDependencies() {
        return totalDependencies;
    }

    @Override
    public String toString() {
        return "RegistryStatistics{" +
            "totalServices=" + totalServices +
            ", totalPlugins=" + totalPlugins +
            ", totalInterfaces=" + totalInterfaces +
            ", totalDependencies=" + totalDependencies +
            '}';
    }
}
