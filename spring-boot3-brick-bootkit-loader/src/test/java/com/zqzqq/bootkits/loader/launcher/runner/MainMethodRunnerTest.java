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


package com.zqzqq.bootkits.loader.launcher.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 主程序方法运行器单元测试
 *
 * @author zqzqq
 * @since 4.1.0
 */
class MainMethodRunnerTest {

    private MainMethodRunner runner;
    private String[] testArgs = {"arg1", "arg2", "arg3"};
    
    @BeforeEach
    void setUp() {
        runner = new MainMethodRunner("com.example.MainClass", "main", testArgs);
    }
    
    @Test
    void testConstructor() {
        assertEquals("com.example.MainClass", runner.getMainClass());
        assertEquals("main", runner.getMainRunMethod());
        assertArrayEquals(testArgs, runner.getArgs());
    }
    
    @Test
    void testGetMainClass() {
        assertEquals("com.example.MainClass", runner.getMainClass());
    }
    
    @Test
    void testGetMainRunMethod() {
        assertEquals("main", runner.getMainRunMethod());
    }
    
    @Test
    void testGetArgs() {
        assertArrayEquals(testArgs, runner.getArgs());
    }
    
    @Test
    void testGetInstance_NullReturned() throws Exception {
        // 对于主程序方法，通常返回null
        Object instance = runner.getInstance(Object.class);
        assertNull(instance);
    }
    
    @Test
    void testGetInstance_DifferentMainClasses() throws Exception {
        // 测试不同的main class名称
        MainMethodRunner runner1 = new MainMethodRunner("MyApp", "start", new String[]{});
        MainMethodRunner runner2 = new MainMethodRunner("AnotherApp", "run", new String[]{"test"});
        
        assertEquals("MyApp", runner1.getMainClass());
        assertEquals("AnotherApp", runner2.getMainClass());
        assertEquals("start", runner1.getMainRunMethod());
        assertEquals("run", runner2.getMainRunMethod());
    }
    
    @Test
    void testGetInstance_EmptyArgs() throws Exception {
        MainMethodRunner runnerWithNoArgs = new MainMethodRunner("com.example.MainClass", "main", new String[]{});
        
        Object instance = runnerWithNoArgs.getInstance(Object.class);
        assertNull(instance);
    }
    
    @Test
    void testGetInstance_NullArgs() throws Exception {
        MainMethodRunner runnerWithNullArgs = new MainMethodRunner("com.example.MainClass", "main", null);
        
        Object instance = runnerWithNullArgs.getInstance(Object.class);
        assertNull(instance);
    }
    
    @Test
    void testEqualsAndHashCode() {
        // 创建具有相同参数的runner
        MainMethodRunner runner1 = new MainMethodRunner("com.example.MainClass", "main", testArgs);
        MainMethodRunner runner2 = new MainMethodRunner("com.example.MainClass", "main", testArgs);
        MainMethodRunner runner3 = new MainMethodRunner("different.MainClass", "main", testArgs);
        
        // 相等的runner应该有相等的hashCode
        assertEquals(runner1, runner2);
        assertEquals(runner2, runner1); // 对称性
        assertEquals(runner1.hashCode(), runner2.hashCode());
        
        // 不相等的runner
        assertNotEquals(runner1, runner3);
        assertNotEquals(runner1, null);
        assertNotEquals(runner1, "not a runner");
        // 注意：不要求hashCode不同，因为hash碰撞是允许的
    }
    
    @Test
    void testToString() {
        String toString = runner.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("MainMethodRunner"));
        assertTrue(toString.contains("mainClass='com.example.MainClass'"));
        assertTrue(toString.contains("mainRunMethod='main'"));
    }
}