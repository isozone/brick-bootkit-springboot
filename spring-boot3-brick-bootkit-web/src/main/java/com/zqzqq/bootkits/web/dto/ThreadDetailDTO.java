package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 线程详细信息 DTO
 * 用于线程监控页面返回完整数据
 *
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 线程统计信息
     */
    private MonitorOverviewDTO.ThreadInfo threadInfo;

    /**
     * 线程列表
     */
    private List<ThreadDetail> threads;

    /**
     * 死锁线程列表
     */
    private List<DeadlockedThread> deadlockedThreads;

    /**
     * 线程详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 线程 ID */
        private long threadId;

        /** 线程名称 */
        private String threadName;

        /** 线程优先级 */
        private int priority;

        /** 线程状态 */
        private String threadState;

        /** 是否守护线程 */
        private boolean daemon;

        /** CPU 时间（纳秒） */
        private long cpuTime;

        /** 阻塞计数器 */
        private long blockedCount;

        /** 阻塞时间（毫秒） */
        private long blockedTime;

        /** 等待计数器 */
        private long waitedCount;

        /** 等待时间（毫秒） */
        private long waitedTime;

        /** 线程用户（通常为空） */
        private String threadUser;

        /** 锁名称 */
        private String lockName;

        /** 锁拥有者线程 ID */
        private long lockOwnerId;

        /** 锁拥有者线程名称 */
        private String lockOwnerName;
    }

    /**
     * 死锁线程信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeadlockedThread implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 线程 ID */
        private long threadId;

        /** 线程名称 */
        private String threadName;

        /** 线程状态 */
        private String threadState;

        /** 优先级 */
        private int priority;

        /** 是否守护线程 */
        private boolean daemon;

        /** CPU 时间（纳秒） */
        private long cpuTime;

        /** 持有的锁 */
        private String lockName;

        /** 等待的锁 */
        private String lockWaitingName;

        /** 锁拥有者线程 ID */
        private long lockOwnerId;

        /** 锁拥有者线程名称 */
        private String lockOwnerName;

        /** 栈跟踪 */
        private String[] stackTrace;
    }
}
