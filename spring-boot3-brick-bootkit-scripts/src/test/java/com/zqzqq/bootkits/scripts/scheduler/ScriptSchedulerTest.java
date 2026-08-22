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


package com.zqzqq.bootkits.scripts.scheduler;

import com.zqzqq.bootkits.scripts.core.*;
import com.zqzqq.bootkits.scripts.core.impl.DefaultScriptManager;
import com.zqzqq.bootkits.scripts.scheduler.impl.ScriptSchedulerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚本调度器测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("脚本调度器测试")
class ScriptSchedulerTest {

    private ScriptScheduler scheduler;
    private DefaultScriptManager scriptManager;
    
    @BeforeEach
    void setUp() throws Exception {
        scriptManager = new DefaultScriptManager();
        scriptManager.initialize();
        scheduler = new ScriptSchedulerImpl(scriptManager);
        scheduler.start();
    }
    
    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    @Test
    @DisplayName("测试调度器启动和停止")
    void testSchedulerStartAndStop() {
        // 调度器已在BeforeEach中启动，这里测试停止
        ScriptScheduler.SchedulerResult stopResult = scheduler.stop();
        assertThat(stopResult.isSuccess()).isTrue();
        assertThat(stopResult.getMessage()).contains("停止成功");
        
        // 再次停止应该也成功
        ScriptScheduler.SchedulerResult stopResult2 = scheduler.stop();
        assertThat(stopResult2.isSuccess()).isTrue();
        assertThat(stopResult2.getMessage()).contains("已停止");
    }

    @Test
    @DisplayName("测试一次性任务调度")
    void testOnceTaskScheduling() {
        // 创建一次性任务
        ScriptScheduler.ScheduledTask task = ScriptScheduler.ScheduledTask.once(
            "once-task-1", "test-script.sh", new String[]{"arg1"},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(2)
        );
        
        // 调度任务
        ScriptScheduler.SchedulerResult result = scheduler.scheduleTask(task);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("调度成功");
        
        // 验证任务状态
        Optional<ScriptScheduler.TaskStatus> status = scheduler.getTaskStatus("once-task-1");
        assertThat(status).isPresent();
        assertThat(status.get().getTaskId()).isEqualTo("once-task-1");
        assertThat(status.get().getScheduleType()).isEqualTo(ScriptScheduler.ScheduleType.ONCE);
    }

    @Test
    @DisplayName("测试间隔任务调度")
    void testIntervalTaskScheduling() {
        // 创建间隔任务
        ScriptScheduler.ScheduledTask task = ScriptScheduler.ScheduledTask.interval(
            "interval-task-1", "test-script.py", new String[]{"arg1"},
            new ScriptConfiguration(), 5 // 5秒间隔
        );
        
        // 调度任务
        ScriptScheduler.SchedulerResult result = scheduler.scheduleTask(task);
        assertThat(result.isSuccess()).isTrue();
        
        // 验证任务状态
        Optional<ScriptScheduler.TaskStatus> status = scheduler.getTaskStatus("interval-task-1");
        assertThat(status).isPresent();
        assertThat(status.get().getScheduleType()).isEqualTo(ScriptScheduler.ScheduleType.INTERVAL);
    }

    @Test
    @DisplayName("测试任务取消")
    void testTaskCancellation() {
        // 创建并调度任务
        ScriptScheduler.ScheduledTask task = ScriptScheduler.ScheduledTask.once(
            "cancel-task-1", "test-script.sh", new String[]{},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(10)
        );
        scheduler.scheduleTask(task);
        
        // 取消任务
        ScriptScheduler.SchedulerResult result = scheduler.cancelTask("cancel-task-1");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("取消成功");
        
        // 验证任务状态已更新
        Optional<ScriptScheduler.TaskStatus> status = scheduler.getTaskStatus("cancel-task-1");
        assertThat(status).isPresent();
        assertThat(status.get().getState()).isEqualTo(ScriptScheduler.TaskState.CANCELLED);
    }

    @Test
    @DisplayName("测试重复任务ID")
    void testDuplicateTaskId() {
        // 创建两个相同ID的任务
        ScriptScheduler.ScheduledTask task1 = ScriptScheduler.ScheduledTask.once(
            "duplicate-task", "test-script.sh", new String[]{},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(5)
        );
        
        ScriptScheduler.ScheduledTask task2 = ScriptScheduler.ScheduledTask.once(
            "duplicate-task", "test-script2.sh", new String[]{},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(10)
        );
        
        // 第一个任务应该成功
        ScriptScheduler.SchedulerResult result1 = scheduler.scheduleTask(task1);
        assertThat(result1.isSuccess()).isTrue();
        
        // 第二个任务应该失败
        ScriptScheduler.SchedulerResult result2 = scheduler.scheduleTask(task2);
        assertThat(result2.isSuccess()).isFalse();
        assertThat(result2.getMessage()).contains("已存在");
    }

    @Test
    @DisplayName("测试任务状态查询")
    void testTaskStatusQuery() {
        // 创建多个任务
        ScriptScheduler.ScheduledTask task1 = ScriptScheduler.ScheduledTask.once(
            "status-task-1", "test-script.sh", new String[]{},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(5)
        );
        
        ScriptScheduler.ScheduledTask task2 = ScriptScheduler.ScheduledTask.interval(
            "status-task-2", "test-script.py", new String[]{},
            new ScriptConfiguration(), 10
        );
        
        scheduler.scheduleTask(task1);
        scheduler.scheduleTask(task2);
        
        // 获取所有任务
        List<ScriptScheduler.TaskStatus> allTasks = scheduler.getAllTasks();
        assertThat(allTasks).hasSize(2);
        
        // 验证任务状态
        Optional<ScriptScheduler.TaskStatus> status1 = scheduler.getTaskStatus("status-task-1");
        Optional<ScriptScheduler.TaskStatus> status2 = scheduler.getTaskStatus("status-task-2");
        
        assertThat(status1).isPresent();
        assertThat(status2).isPresent();
        assertThat(status1.get().getState()).isEqualTo(ScriptScheduler.TaskState.SCHEDULED);
        assertThat(status2.get().getState()).isEqualTo(ScriptScheduler.TaskState.SCHEDULED);
    }

    @Test
    @DisplayName("测试脚本队列")
    void testScriptQueue() {
        // 添加多个脚本到队列
        ScriptScheduler.QueueItem item1 = ScriptScheduler.QueueItem.normal(
            "queue-item-1", "test-script.sh", new String[]{"arg1"},
            new ScriptConfiguration(), "test-user"
        );
        
        ScriptScheduler.QueueItem item2 = ScriptScheduler.QueueItem.high(
            "queue-item-2", "test-script.py", new String[]{"arg2"},
            new ScriptConfiguration(), "test-user"
        );
        
        // 添加到队列
        ScriptScheduler.SchedulerResult result1 = scheduler.enqueueScript(item1);
        ScriptScheduler.SchedulerResult result2 = scheduler.enqueueScript(item2);
        
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
        
        // 获取队列状态
        ScriptScheduler.QueueStatus queueStatus = scheduler.getQueueStatus();
        assertThat(queueStatus.getTotalItems()).isGreaterThanOrEqualTo(2);
        assertThat(queueStatus.getPendingItems()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("测试批处理作业")
    void testBatchJob() {
        // 创建批处理作业
        List<String> scripts = List.of("test-script.sh", "test-script.py", "test-script.lua");
        ScriptScheduler.BatchJob job = new ScriptScheduler.BatchJob(
            "batch-job-1", "测试批处理作业", scripts,
            ScriptScheduler.BatchExecutionMode.SEQUENTIAL, false, 2,
            LocalDateTime.now(), "test-user", "测试批处理作业"
        );
        
        // 添加作业
        ScriptScheduler.SchedulerResult result = scheduler.addBatchJob(job);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("已添加");
        
        // 获取作业状态
        Optional<ScriptScheduler.JobStatus> jobStatus = scheduler.getJobStatus("batch-job-1");
        assertThat(jobStatus).isPresent();
        assertThat(jobStatus.get().getJobId()).isEqualTo("batch-job-1");
        assertThat(jobStatus.get().getTotalScripts()).isEqualTo(3);
    }

    @Test
    @DisplayName("测试队列优先级")
    void testQueuePriority() {
        // 创建不同优先级的队列项
        ScriptScheduler.QueueItem lowPriority = ScriptScheduler.QueueItem.low(
            "low-priority", "test-script.sh", new String[]{},
            new ScriptConfiguration(), "test-user"
        );
        
        ScriptScheduler.QueueItem normalPriority = ScriptScheduler.QueueItem.normal(
            "normal-priority", "test-script.py", new String[]{},
            new ScriptConfiguration(), "test-user"
        );
        
        ScriptScheduler.QueueItem highPriority = ScriptScheduler.QueueItem.high(
            "high-priority", "test-script.lua", new String[]{},
            new ScriptConfiguration(), "test-user"
        );
        
        ScriptScheduler.QueueItem urgentPriority = new ScriptScheduler.QueueItem(
            "urgent-priority", "test-script.ps1", new String[]{},
            new ScriptConfiguration(), ScriptScheduler.QueuePriority.URGENT,
            LocalDateTime.now(), "test-user"
        );
        
        // 添加到队列
        scheduler.enqueueScript(lowPriority);
        scheduler.enqueueScript(normalPriority);
        scheduler.enqueueScript(highPriority);
        scheduler.enqueueScript(urgentPriority);
        
        // 验证优先级
        assertThat(lowPriority.getPriority().getLevel()).isEqualTo(1);
        assertThat(normalPriority.getPriority().getLevel()).isEqualTo(2);
        assertThat(highPriority.getPriority().getLevel()).isEqualTo(3);
        assertThat(urgentPriority.getPriority().getLevel()).isEqualTo(4);
    }

    @Test
    @DisplayName("测试调度器未运行状态")
    void testSchedulerNotRunning() {
        // 停止调度器
        scheduler.stop();
        
        // 尝试在停止状态下调度任务
        ScriptScheduler.ScheduledTask task = ScriptScheduler.ScheduledTask.once(
            "not-running-task", "test-script.sh", new String[]{},
            new ScriptConfiguration(), LocalDateTime.now().plusSeconds(5)
        );
        
        ScriptScheduler.SchedulerResult result = scheduler.scheduleTask(task);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("未运行");
    }

    @Test
    @DisplayName("测试无效任务信息")
    void testInvalidTaskInfo() {
        // 测试null任务
        ScriptScheduler.SchedulerResult result1 = scheduler.scheduleTask(null);
        assertThat(result1.isSuccess()).isFalse();
        assertThat(result1.getMessage()).contains("不能为空");
        
        // 测试null队列项
        ScriptScheduler.SchedulerResult result2 = scheduler.enqueueScript(null);
        assertThat(result2.isSuccess()).isFalse();
        assertThat(result2.getMessage()).contains("不能为空");
        
        // 测试null批处理作业
        ScriptScheduler.SchedulerResult result3 = scheduler.addBatchJob(null);
        assertThat(result3.isSuccess()).isFalse();
        assertThat(result3.getMessage()).contains("不能为空");
    }

    @Test
    @DisplayName("测试Cron任务调度")
    void testCronTaskScheduling() {
        // 创建Cron任务
        ScriptScheduler.ScheduledTask cronTask = ScriptScheduler.ScheduledTask.cron(
            "cron-task-1", "test-script.sh", new String[]{},
            new ScriptConfiguration(), "0 */5 * * * *" // 每5分钟执行一次
        );
        
        // 调度任务
        ScriptScheduler.SchedulerResult result = scheduler.scheduleTask(cronTask);
        assertThat(result.isSuccess()).isTrue();
        
        // 验证任务状态
        Optional<ScriptScheduler.TaskStatus> status = scheduler.getTaskStatus("cron-task-1");
        assertThat(status).isPresent();
        assertThat(status.get().getScheduleType()).isEqualTo(ScriptScheduler.ScheduleType.CRON);
    }
}