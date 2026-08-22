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


package com.zqzqq.bootkits.distributed.serialization;

import org.slf4j.MDC;

/**
 * 链路追踪 ID 透传工具。
 * <p>
 * 通过 SLF4J MDC 读写 traceId。宿主调用远端插件前读取当前线程 MDC 中的 traceId，
 * 放入 gRPC 请求头；执行节点在 invoke 线程中将其写回本线程的 MDC，
 * 使远端插件内打出的日志与调用方处于同一条链路。
 */
public final class DistTrace {

    /** MDC/请求头中使用的 traceId 键名。 */
    public static final String KEY = "traceId";
    /** gRPC 请求头中透传 traceId 的键名。 */
    public static final String HEADER_NAME = "x-trace-id";

    private DistTrace() {
    }

    /**
     * 读取当前线程 MDC 中的 traceId。
     */
    public static String get() {
        return MDC.get(KEY);
    }

    /**
     * 将 traceId 写入当前线程 MDC。若传入为空，则移除。
     */
    public static void put(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(KEY, traceId);
        } else {
            MDC.remove(KEY);
        }
    }

    /**
     * 若当前线程尚无 traceId，则生成一个新的（UUID 简写）。
     */
    public static String getOrCreate() {
        String current = MDC.get(KEY);
        if (current != null && !current.isEmpty()) {
            return current;
        }
        String generated = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(KEY, generated);
        return generated;
    }
}