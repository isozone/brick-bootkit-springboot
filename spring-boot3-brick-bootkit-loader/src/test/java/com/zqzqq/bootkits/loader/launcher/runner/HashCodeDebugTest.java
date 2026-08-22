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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 哈希码调试测试
 */
class HashCodeDebugTest {
    
    @Test
    void testHashCodeConsistency() {
        String[] args1 = {"arg1", "arg2", "arg3"};
        String[] args2 = {"arg1", "arg2", "arg3"};
        
        MainMethodRunner runner1 = new MainMethodRunner("com.example.MainClass", "main", args1);
        MainMethodRunner runner2 = new MainMethodRunner("com.example.MainClass", "main", args2);
        
        System.out.println("Runner1 args hashCode: " + System.identityHashCode(args1));
        System.out.println("Runner2 args hashCode: " + System.identityHashCode(args2));
        System.out.println("Runner1 hashCode: " + runner1.hashCode());
        System.out.println("Runner2 hashCode: " + runner2.hashCode());
        System.out.println("runner1.equals(runner2): " + runner1.equals(runner2));
        System.out.println("Arrays.equals(args1, args2): " + java.util.Arrays.equals(args1, args2));
        
        assertTrue(runner1.equals(runner2));
        assertEquals(runner1.hashCode(), runner2.hashCode());
    }
    
    @Test
    void testSameReference() {
        String[] args = {"arg1", "arg2", "arg3"};
        
        MainMethodRunner runner1 = new MainMethodRunner("com.example.MainClass", "main", args);
        MainMethodRunner runner2 = new MainMethodRunner("com.example.MainClass", "main", args);
        
        System.out.println("Same reference test:");
        System.out.println("Runner1 hashCode: " + runner1.hashCode());
        System.out.println("Runner2 hashCode: " + runner2.hashCode());
        
        assertTrue(runner1.equals(runner2));
        assertEquals(runner1.hashCode(), runner2.hashCode());
    }
}
