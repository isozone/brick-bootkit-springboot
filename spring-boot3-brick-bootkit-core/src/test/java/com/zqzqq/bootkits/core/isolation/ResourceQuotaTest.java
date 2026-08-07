package com.zqzqq.bootkits.core.isolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资源配额测试
 */
@DisplayName("ResourceQuota Test")
class ResourceQuotaTest {

    @Test
    @DisplayName("测试创建默认资源配额")
    void testCreateDefaultResourceQuota() {
        ResourceQuota quota = ResourceQuota.defaultQuota();

        assertThat(quota.getMaxMemoryBytes()).isEqualTo(256 * 1024 * 1024); // 256MB
        assertThat(quota.getMaxThreads()).isEqualTo(20);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(50.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(100);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(1000);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(10 * 1024 * 1024); // 10MB/s
    }

    @Test
    @DisplayName("测试创建空构建器默认值")
    void testEmptyBuilderDefaults() {
        ResourceQuota quota = ResourceQuota.newBuilder().build();

        // 未显式设置的配额默认均为 0（表示未限制）
        assertThat(quota.getMaxMemoryBytes()).isEqualTo(0);
        assertThat(quota.getMaxThreads()).isEqualTo(0);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(0.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(0);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(0);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("测试设置内存配额")
    void testSetMemoryQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024) // 512MB
            .build();

        assertThat(quota.getMaxMemoryBytes()).isEqualTo(512 * 1024 * 1024);
    }

    @Test
    @DisplayName("测试设置线程配额")
    void testSetThreadQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxThreads(50)
            .build();

        assertThat(quota.getMaxThreads()).isEqualTo(50);
    }

    @Test
    @DisplayName("测试设置CPU配额")
    void testSetCpuQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxCpuPercent(80.0)
            .build();

        assertThat(quota.getMaxCpuPercent()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("测试设置网络连接配额")
    void testSetNetworkQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxNetworkConnections(200)
            .build();

        assertThat(quota.getMaxNetworkConnections()).isEqualTo(200);
    }

    @Test
    @DisplayName("测试设置文件描述符配额")
    void testSetFileDescriptorQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxFileDescriptors(2000)
            .build();

        assertThat(quota.getMaxFileDescriptors()).isEqualTo(2000);
    }

    @Test
    @DisplayName("测试设置磁盘I/O配额")
    void testSetDiskIOQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxDiskIOBytesPerSecond(20 * 1024 * 1024) // 20MB/s
            .build();

        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(20 * 1024 * 1024);
    }

    @Test
    @DisplayName("测试设置所有配额")
    void testSetAllQuotas() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(1024 * 1024 * 1024) // 1GB
            .setMaxThreads(100)
            .setMaxCpuPercent(90.0)
            .setMaxNetworkConnections(500)
            .setMaxFileDescriptors(5000)
            .setMaxDiskIOBytesPerSecond(50 * 1024 * 1024) // 50MB/s
            .build();

        assertThat(quota.getMaxMemoryBytes()).isEqualTo(1024 * 1024 * 1024);
        assertThat(quota.getMaxThreads()).isEqualTo(100);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(90.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(500);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(5000);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(50 * 1024 * 1024);
    }

    @Test
    @DisplayName("测试无效的内存配额")
    void testInvalidMemoryQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxMemoryBytes(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无效的线程配额")
    void testInvalidThreadQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxThreads(-5)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无效的CPU配额")
    void testInvalidCpuQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxCpuPercent(-10.0)
            .build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxCpuPercent(150.0)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无效的网络连接配额")
    void testInvalidNetworkQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxNetworkConnections(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无效的文件描述符配额")
    void testInvalidFileDescriptorQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxFileDescriptors(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无效的磁盘I/O配额")
    void testInvalidDiskIOQuota() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder()
            .setMaxDiskIOBytesPerSecond(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试无限制配额")
    void testUnlimitedQuota() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(0)
            .setMaxThreads(0)
            .setMaxCpuPercent(0.0)
            .setMaxNetworkConnections(0)
            .setMaxFileDescriptors(0)
            .setMaxDiskIOBytesPerSecond(0)
            .build();

        assertThat(quota.getMaxMemoryBytes()).isEqualTo(0);
        assertThat(quota.getMaxThreads()).isEqualTo(0);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(0.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(0);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(0);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("测试边界值配额")
    void testBoundaryValueQuotas() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(Long.MAX_VALUE)
            .setMaxThreads(Integer.MAX_VALUE)
            .setMaxCpuPercent(100.0)
            .setMaxNetworkConnections(Integer.MAX_VALUE)
            .setMaxFileDescriptors(Integer.MAX_VALUE)
            .setMaxDiskIOBytesPerSecond(Long.MAX_VALUE)
            .build();

        assertThat(quota.getMaxMemoryBytes()).isEqualTo(Long.MAX_VALUE);
        assertThat(quota.getMaxThreads()).isEqualTo(Integer.MAX_VALUE);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(100.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(Integer.MAX_VALUE);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(Integer.MAX_VALUE);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("测试配额对象的不可变性")
    void testQuotaImmutability() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024)
            .setMaxThreads(50)
            .build();

        // 一旦创建，配额对象不应该被修改
        assertThat(quota.getMaxMemoryBytes()).isEqualTo(512 * 1024 * 1024);
        assertThat(quota.getMaxThreads()).isEqualTo(50);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(0.0); // 未设置，默认 0
    }

    @Test
    @DisplayName("测试配额对象的独立性")
    void testQuotaObjectIndependence() {
        ResourceQuota quota1 = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024)
            .setMaxThreads(50)
            .build();

        ResourceQuota quota2 = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024)
            .setMaxThreads(50)
            .build();

        ResourceQuota quota3 = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(1024 * 1024 * 1024)
            .setMaxThreads(50)
            .build();

        // 每次构建都是独立对象
        assertThat(quota1).isNotSameAs(quota2);
        assertThat(quota1.getMaxMemoryBytes()).isEqualTo(quota2.getMaxMemoryBytes());
        assertThat(quota1.getMaxMemoryBytes()).isNotEqualTo(quota3.getMaxMemoryBytes());
    }

    @Test
    @DisplayName("测试配额对象的toString")
    void testQuotaToString() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024)
            .setMaxThreads(50)
            .setMaxCpuPercent(80.0)
            .build();

        String str = quota.toString();
        assertThat(str).contains("ResourceQuota");
        assertThat(str).contains("memory=512MB");
        assertThat(str).contains("threads=50");
        assertThat(str).contains("cpu=80.0%");
    }

    @Test
    @DisplayName("测试复杂配额场景")
    void testComplexQuotaScenarios() {
        ResourceQuota highPerformanceQuota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(2048L * 1024 * 1024) // 2GB
            .setMaxThreads(200)
            .setMaxCpuPercent(95.0)
            .setMaxNetworkConnections(1000)
            .setMaxFileDescriptors(10000)
            .setMaxDiskIOBytesPerSecond(100L * 1024 * 1024) // 100MB/s
            .build();

        assertThat(highPerformanceQuota.getMaxMemoryBytes()).isEqualTo(2048L * 1024 * 1024);
        assertThat(highPerformanceQuota.getMaxThreads()).isEqualTo(200);
        assertThat(highPerformanceQuota.getMaxCpuPercent()).isEqualTo(95.0);
        assertThat(highPerformanceQuota.getMaxNetworkConnections()).isEqualTo(1000);
        assertThat(highPerformanceQuota.getMaxFileDescriptors()).isEqualTo(10000);
        assertThat(highPerformanceQuota.getMaxDiskIOBytesPerSecond()).isEqualTo(100L * 1024 * 1024);

        ResourceQuota lightweightQuota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(64L * 1024 * 1024) // 64MB
            .setMaxThreads(5)
            .setMaxCpuPercent(20.0)
            .setMaxNetworkConnections(10)
            .setMaxFileDescriptors(100)
            .setMaxDiskIOBytesPerSecond(1L * 1024 * 1024) // 1MB/s
            .build();

        assertThat(lightweightQuota.getMaxMemoryBytes()).isEqualTo(64L * 1024 * 1024);
        assertThat(lightweightQuota.getMaxThreads()).isEqualTo(5);
        assertThat(lightweightQuota.getMaxCpuPercent()).isEqualTo(20.0);
        assertThat(lightweightQuota.getMaxNetworkConnections()).isEqualTo(10);
        assertThat(lightweightQuota.getMaxFileDescriptors()).isEqualTo(100);
        assertThat(lightweightQuota.getMaxDiskIOBytesPerSecond()).isEqualTo(1024L * 1024);
    }

    @Test
    @DisplayName("测试配额构建器的链式调用")
    void testQuotaBuilderMethodChaining() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(512 * 1024 * 1024)
            .setMaxThreads(50)
            .setMaxCpuPercent(80.0)
            .setMaxNetworkConnections(200)
            .setMaxFileDescriptors(1000)
            .setMaxDiskIOBytesPerSecond(20 * 1024 * 1024)
            .build();

        assertThat(quota).isNotNull();
        assertThat(quota.getMaxMemoryBytes()).isEqualTo(512L * 1024 * 1024);
        assertThat(quota.getMaxThreads()).isEqualTo(50);
        assertThat(quota.getMaxCpuPercent()).isEqualTo(80.0);
        assertThat(quota.getMaxNetworkConnections()).isEqualTo(200);
        assertThat(quota.getMaxFileDescriptors()).isEqualTo(1000);
        assertThat(quota.getMaxDiskIOBytesPerSecond()).isEqualTo(20L * 1024 * 1024);
    }

    @Test
    @DisplayName("测试配额验证逻辑")
    void testQuotaValidationLogic() {
        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxMemoryBytes(-1).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxThreads(-1).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxCpuPercent(-1).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxCpuPercent(101).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxNetworkConnections(-1).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxFileDescriptors(-1).build())
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ResourceQuota.newBuilder().setMaxDiskIOBytesPerSecond(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试配额对象的内存效率")
    void testQuotaMemoryEfficiency() {
        int count = 10000;
        ResourceQuota[] quotas = new ResourceQuota[count];

        for (int i = 0; i < count; i++) {
            quotas[i] = ResourceQuota.newBuilder()
                .setMaxMemoryBytes(i * 1024L * 1024)
                .setMaxThreads(i % 100 + 1)
                .build();
        }

        assertThat(quotas[0].getMaxMemoryBytes()).isEqualTo(0);
        assertThat(quotas[count - 1].getMaxMemoryBytes()).isEqualTo((count - 1) * 1024L * 1024);
        assertThat(quotas[0].getMaxThreads()).isEqualTo(1);
        assertThat(quotas[count - 1].getMaxThreads()).isEqualTo((count - 1) % 100 + 1);
    }

    @Test
    @DisplayName("测试配额限制检查方法")
    void testQuotaLimitChecks() {
        ResourceQuota quota = ResourceQuota.newBuilder()
            .setMaxMemoryBytes(1024)
            .setMaxThreads(10)
            .build();

        assertThat(quota.hasMemoryLimit()).isTrue();
        assertThat(quota.hasThreadLimit()).isTrue();
        assertThat(quota.hasFileDescriptorLimit()).isFalse();
        assertThat(quota.hasCpuLimit()).isFalse();
        assertThat(quota.hasNetworkConnectionLimit()).isFalse();
        assertThat(quota.hasDiskIOLimit()).isFalse();
    }
}
