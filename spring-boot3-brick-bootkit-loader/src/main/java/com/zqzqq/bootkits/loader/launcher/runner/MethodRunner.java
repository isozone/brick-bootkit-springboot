/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.loader.launcher.runner;

import com.zqzqq.bootkits.utils.CompareClassTypeUtils;
import com.zqzqq.bootkits.loader.utils.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 反射杩愯方法
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class MethodRunner {

    protected final String className;
    protected final String runMethodName;

    protected String[] args;

    public MethodRunner(String className, String runMethodName, String[] args) {
        this.className = checkEmpty(className, "className 不能为空");
        this.runMethodName = checkEmpty(runMethodName, "runMethod 不能为空");
        this.args = (args != null) ? args.clone() : null;
    }

    public Object run() throws Exception {
        return run(null);
    }

    public Object run(ClassLoader classLoader) throws Exception {
        Class<?> runClass = loadRunClass(classLoader);
        return runMethod(runClass);
    }

    protected Class<?> loadRunClass(ClassLoader classLoader) throws Exception{
        if(classLoader == null){
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return Class.forName(this.className, false, classLoader);
    }

    protected Object runMethod(Class<?> runClass) throws Exception {
        Method runMethod = findRunMethod(runClass);
        if(runMethod == null) {
            throw new NoSuchMethodException(runClass.getName() + "." + runMethodName + "(String[] args)");
        }
        Object instance = getInstance(runClass);
        runMethod.setAccessible(true);
        runMethod.invoke(instance, new Object[] { this.args });
        return instance;
    }

    protected Object getInstance(Class<?> runClass) throws Exception {
        return runClass.getConstructor().newInstance();
    }

    private String checkEmpty(String value, String msg){
        if(ObjectUtils.isEmpty(value)){
            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    private Method findRunMethod(Class<?> runClass){
        Class<?> searchType = runClass;
        Class<?>[] argClasses = new Class[]{ String[].class };
        while (searchType != null) {
            Method[] methods = searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods();
            for (Method method : methods) {
                if(!Objects.equals(method.getName(), runMethodName)){
                    continue;
                }
                if(hasSameParams(method, argClasses)){
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private boolean hasSameParams(Method method, Class<?>[] paramTypes) {
        if(paramTypes.length != method.getParameterCount()){
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            Class<?> methodParamType = parameterTypes[i];
            if(CompareClassTypeUtils.compare(methodParamType, paramType)){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取主类名
     * 
     * @return 主类名
     */
    public String getMainClass() {
        return className;
    }

    /**
     * 获取主运行方法名
     * 
     * @return 主运行方法名
     */
    public String getMainRunMethod() {
        return runMethodName;
    }

    /**
     * 获取参数数组
     * 
     * @return 参数数组
     */
    public String[] getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MethodRunner that = (MethodRunner) obj;
        return Objects.equals(className, that.className) &&
               Objects.equals(runMethodName, that.runMethodName) &&
               Objects.deepEquals(args, that.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(className, runMethodName);
        if (args != null) {
            result = result * 31 + java.util.Arrays.hashCode(args);
        }
        return result;
    }

    @Override
    public String toString() {
        return "MethodRunner{" +
                "mainClass='" + className + '\'' +
                ", mainRunMethod='" + runMethodName + '\'' +
                ", args=" + java.util.Arrays.toString(args) +
                '}';
    }

}

