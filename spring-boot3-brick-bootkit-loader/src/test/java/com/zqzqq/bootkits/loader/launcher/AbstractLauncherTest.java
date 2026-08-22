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


package com.zqzqq.bootkits.loader.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 抽象启动器单元测试
 *
 * @author zqzqq
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class AbstractLauncherTest {

    @Mock
    private ClassLoader mockClassLoader;
    
    private TestLauncher launcher;
    
    @BeforeEach
    void setUp() {
        launcher = new TestLauncher(mockClassLoader, "test-result");
        launcher.resetClassLoaderRestored();
    }
    
    @Test
    void testRun_Successful() throws Exception {
        String[] args = {"arg1", "arg2"};
        
        String result = launcher.run(args);
        
        assertEquals("test-result", result);
        
        // 验证classLoader创建被调用
        assertTrue(launcher.isCreateClassLoaderCalled());
        assertEquals(args, launcher.getLastArgs());
        
        // 验证类加载器被正确设置和恢复
        // verify(mockClassLoader, times(1)).getClassLoader();
    }
    
    @Test
    void testRun_ExceptionDuringClassLoaderCreation() throws Exception {
        TestLauncher failingLauncher = new TestLauncher(null, "test-result");
        failingLauncher.setShouldFailOnCreateClassLoader(true);
        
        assertThrows(RuntimeException.class, () -> {
            failingLauncher.run("arg1", "arg2");
        });
    }
    
    @Test
    void testRun_ExceptionDuringLaunch() throws Exception {
        TestLauncher failingLauncher = new TestLauncher(mockClassLoader, "test-result");
        failingLauncher.setShouldFailOnLaunch(true);
        
        assertThrows(RuntimeException.class, () -> {
            failingLauncher.run("arg1", "arg2");
        });
    }
    
    @Test
    void testRun_ClassLoaderRestorationOnException() throws Exception {
        TestLauncher failingLauncher = new TestLauncher(mockClassLoader, "test-result");
        failingLauncher.setShouldFailOnLaunch(true);
        failingLauncher.resetClassLoaderRestored();
        
        try {
            failingLauncher.run("arg1", "arg2");
            fail("Expected exception");
        } catch (RuntimeException e) {
            // 验证类加载器确实在异常情况下被恢复
            assertTrue(failingLauncher.isClassLoaderRestored());
        }
    }
    
    /**
     * 测试用的启动器实现
     */
    private static class TestLauncher extends AbstractLauncher<String> {
        
        private final ClassLoader classLoaderToReturn;
        private final String resultToReturn;
        private boolean shouldFailOnCreateClassLoader = false;
        private boolean shouldFailOnLaunch = false;
        String[] lastArgs; // Package-private for test access
        
        public TestLauncher(ClassLoader classLoaderToReturn, String resultToReturn) {
            super();
            this.classLoaderToReturn = classLoaderToReturn;
            this.resultToReturn = resultToReturn;
        }
        
        public void setShouldFailOnCreateClassLoader(boolean shouldFail) {
            this.shouldFailOnCreateClassLoader = shouldFail;
        }
        
        public void setShouldFailOnLaunch(boolean shouldFail) {
            this.shouldFailOnLaunch = shouldFail;
        }
        
        public boolean isClassLoaderRestored() {
            return super.isClassLoaderRestored();
        }
        
        public String[] getLastArgs() {
            return lastArgs;
        }
        
        public boolean isCreateClassLoaderCalled() {
            return createClassLoaderCalled;
        }
        
        private boolean createClassLoaderCalled = false;
        
        @Override
        protected ClassLoader createClassLoader(String... args) throws Exception {
            createClassLoaderCalled = true;
            this.lastArgs = args;
            
            if (shouldFailOnCreateClassLoader) {
                throw new RuntimeException("Failed to create class loader");
            }
            
            return classLoaderToReturn;
        }
        
        @Override
        protected String launch(ClassLoader classLoader, String... args) throws Exception {
            if (shouldFailOnLaunch) {
                throw new RuntimeException("Failed to launch");
            }
            
            return resultToReturn;
        }
    }
}