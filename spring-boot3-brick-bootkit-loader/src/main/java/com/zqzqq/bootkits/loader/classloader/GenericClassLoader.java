/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.loader.classloader;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.loader.utils.Assert;
import com.zqzqq.bootkits.loader.utils.IOUtils;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用ClassLoader
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class GenericClassLoader extends URLClassLoader implements ResourceLoaderFactory{

    private static final Logger log = LoggerFactory.getLogger(GenericClassLoader.class);
    private final String name;
    private final ClassLoader parent;

    protected final ResourceLoaderFactory resourceLoaderFactory;

    private final ResourceLoaderFactory classLoaderTranslator;

    private final Map<String, Class<?>> pluginClassCache = new ConcurrentHashMap<>();

    public GenericClassLoader(String name, ResourceLoaderFactory resourceLoaderFactory) {
        this(name, Thread.currentThread().getContextClassLoader(), resourceLoaderFactory);
    }

    public GenericClassLoader(String name, ClassLoader parent, ResourceLoaderFactory resourceLoaderFactory) {
        super(new URL[]{}, getSystemClassLoaderWithJavaBase(parent));
        this.name = Assert.isNotEmpty(name, "name 不能为空");
        this.resourceLoaderFactory = Assert.isNotNull(resourceLoaderFactory, "resourceLoaderFactory 不能为空");
        this.parent = parent;
        this.classLoaderTranslator = new ClassLoaderTranslator(this);
        
        // 确保类加载器能够访问Java基础类
        log.debug("创建 GenericClassLoader '{}'，父类加载器: {}", name, getSystemClassLoaderWithJavaBase(parent));
    }

    /**
     * 获取系统类加载器并确保能访问Java基础类
     */
    private static ClassLoader getSystemClassLoaderWithJavaBase(ClassLoader parent) {
        // 首先尝试获取系统类加载器
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        
        if (parent != null) {
            // 如果有自定义父类加载器，使用组合模式
            return parent;
        }
        
        // 如果没有父类加载器，确保至少有系统类加载器
        return systemClassLoader != null ? systemClassLoader : getBootstrapClassLoader();
    }
    
    /**
     * 获取Bootstrap类加载器（用于访问Java基础类）
     */
    private static ClassLoader getBootstrapClassLoader() {
        try {
            // 通过反射获取Bootstrap类加载器
            ClassLoader bootstrapClassLoader = ClassLoader.getSystemClassLoader().getParent();
            if (bootstrapClassLoader != null) {
                return bootstrapClassLoader;
            }
        } catch (Exception e) {
            log.warn("无法获取Bootstrap类加载器: {}", e.getMessage());
        }
        
        // 如果无法获取Bootstrap类加载器，使用系统类加载器
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader != null) {
            return systemClassLoader;
        }
        
        // 最后的回退：使用当前类的类加载器
        ClassLoader currentClassLoader = GenericClassLoader.class.getClassLoader();
        if (currentClassLoader != null) {
            return currentClassLoader;
        }
        
        // 最后的最后：使用应用类加载器
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 确保父类加载器不为空且有效（保留旧方法以保持兼容性）
     */
    private static ClassLoader ensureValidParent(ClassLoader parent) {
        if (parent != null) {
            return parent;
        }
        
        // 回退到系统类加载器
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader != null) {
            return systemClassLoader;
        }
        
        // 最后的回退：使用当前类的类加载器
        ClassLoader currentClassLoader = GenericClassLoader.class.getClassLoader();
        if (currentClassLoader != null) {
            return currentClassLoader;
        }
        
        // 最后的最后：使用应用类加载器
        return Thread.currentThread().getContextClassLoader();
    }

    public String getName() {
        return name;
    }

    public ClassLoader getParentClassLoader(){
        return parent;
    }

    @Override
    public void addResource(String path) throws Exception {
        resourceLoaderFactory.addResource(path);
    }

    @Override
    public void addResource(File file) throws Exception {
        resourceLoaderFactory.addResource(file);
    }

    @Override
    public void addResource(Path path) throws Exception {
        resourceLoaderFactory.addResource(path);
    }

    @Override
    public void addResource(URL url) throws Exception {
        resourceLoaderFactory.addResource(url);
    }

    @Override
    public void addResource(Resource resource) throws Exception {
        resourceLoaderFactory.addResource(resource);
    }

    @Override
    public void addResource(ResourceLoader resourceLoader) throws Exception{
        resourceLoaderFactory.addResource(resourceLoader);
    }

    @Override
    public Resource findFirstResource(String name) {
        return classLoaderTranslator.findFirstResource(name);
    }

    @Override
    public Enumeration<Resource> findAllResource(String name) {
        return classLoaderTranslator.findAllResource(name);
    }

    @Override
    public InputStream getInputStream(String name) {
        return classLoaderTranslator.getInputStream(name);
    }

    @Override
    public List<URL> getUrls() {
        return classLoaderTranslator.getUrls();
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            // Java基础类应该优先由系统类加载器加载
            if (isJavaBaseClass(className)) {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                if (systemClassLoader != null) {
                    return systemClassLoader.loadClass(className);
                }
            }
            return findClass(className);
        }
    }
    
    /**
     * 判断是否为Java基础类
     */
    private boolean isJavaBaseClass(String className) {
        if (className == null) {
            return false;
        }
        
        // Java基础包
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("sun.") ||
               className.startsWith("jdk.") ||
               // 一些特殊的Java基础类
               className.equals("java.lang.Object") ||
               className.equals("java.lang.String") ||
               className.equals("java.lang.Class") ||
               className.equals("java.lang.Thread");
    }

    @Override
    public URL[] getURLs() {
        List<URL> urlList = resourceLoaderFactory.getUrls();
        URL[] urls = new URL[urlList.size()];
        for (int i = 0; i < urlList.size(); i++) {
            urls[i] = urlList.get(i);
        }
        return urls;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        name = formatResourceName(name);
        InputStream inputStream = findInputStreamFromParent(name);
        if(inputStream != null){
            return inputStream;
        }
        return findInputStreamFromLocal(name);
    }

    @Override
    public URL getResource(String name) {
        name = formatResourceName(name);
        URL url = findResourceFromParent(name);
        if(url != null){
            return url;
        }
        return findResourceFromLocal(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        name = formatResourceName(name);
        Enumeration<URL> parentResources = findResourcesFromParent(name);
        Enumeration<URL> localResources = findResourcesFromLocal(name);
        return new Enumeration<URL>() {

            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                if(parentResources != null && parentResources.hasMoreElements()){
                    return true;
                }
                index = 1;
                return localResources.hasMoreElements();
            }

            @Override
            public URL nextElement() {
                if(index == 0){
                    return parentResources.nextElement();
                } else {
                    return localResources.nextElement();
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        try {
            // 清理类缓存，防止内容驻留
            pluginClassCache.clear();
            
            // 关闭资源加载工厂
            if (resourceLoaderFactory != null) {
                try {
                    resourceLoaderFactory.close();
                } catch (Exception e) {
                    log.warn("Failed to close resource loader factory", e);
                }
            }
            
            // 调用父类关闭方法
            super.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("关闭类加载器异常", e);
        }
    }

    @Override
    public void release() {
        try {
            // 清理类缓存
            pluginClassCache.clear();
            
            // 释放资源加载工厂
            ResourceUtils.release(resourceLoaderFactory);
        } catch (Exception e) {
            log.warn("Failed to release resources", e);
        }
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        // Java基础类应该优先由父类加载器处理
        if (isJavaBaseClass(className)) {
            return findClassFromParent(className);
        }
        
        Class<?> loadedClass = findClassFromParent(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        loadedClass = findClassFromLocal(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        throw new ClassNotFoundException("ClassLoader[" + name  +"]:" + className);
    }

    protected Class<?> findClassFromParent(String className) throws ClassNotFoundException{
        if (className == null) {
            throw new ClassNotFoundException("类名为空");
        }
        
        try {
            // Java基础类优先使用系统类加载器
            if (isJavaBaseClass(className)) {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                if (systemClassLoader != null) {
                    return systemClassLoader.loadClass(className);
                }
            }
            
            // 如果有自定义父类加载器，尝试使用
            if(parent != null){
                return parent.loadClass(className);
            }
            
            // 使用系统类加载器
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader != null) {
                return systemClassLoader.loadClass(className);
            }
            
            return null;
        } catch (ClassNotFoundException e) {
            // 对于Java基础类，抛出更详细的错误信息
            if (isJavaBaseClass(className)) {
                throw new ClassNotFoundException("无法加载Java基础类: " + className + "，请检查JVM环境配置", e);
            }
            return null;
        } catch (Exception e){
            return null;
        }
    }

    protected Class<?> findClassFromLocal(String name) {
        Class<?> aClass;
        String formatClassName = formatClassName(name);

        aClass = pluginClassCache.get(formatClassName);
        if (aClass != null) {
            return aClass;
        }

        Resource resource = resourceLoaderFactory.findFirstResource(formatClassName);
        byte[] bytes = null;
        if(resource != null){
            bytes = resource.getBytes();
        }
        if(bytes == null || bytes.length == 0){
            bytes = getClassByte(formatClassName);
        }
        if(bytes == null || bytes.length == 0){
            return null;
        }
        aClass = super.defineClass(name, bytes, 0, bytes.length );
        if(aClass == null) {
            return null;
        }
        if (aClass.getPackage() == null) {
            int lastDotIndex = name.lastIndexOf( '.' );
            String packageName = (lastDotIndex >= 0) ? name.substring( 0, lastDotIndex) : "";
            super.definePackage(packageName, null, null, null,
                    null, null, null, null );
        }
        pluginClassCache.put(name, aClass);
        return aClass;
    }

    protected URL findResourceFromParent(String name){
        if(parent != null){
            return parent.getResource(name);
        }
        return null;
    }

    protected URL findResourceFromLocal(String name) {
        Resource resource = resourceLoaderFactory.findFirstResource(name);
        if (resource == null) {
            return null;
        }
        return resource.getUrl();
    }


    protected InputStream findInputStreamFromParent(String name){
        if(parent != null){
            return parent.getResourceAsStream(name);
        }
        return null;
    }

    protected InputStream findInputStreamFromLocal(String name){
        return resourceLoaderFactory.getInputStream(name);
    }

    protected Enumeration<URL> findResourcesFromParent(String name) throws IOException{
        if(parent != null){
            return parent.getResources(name);
        }
        return null;
    }

    protected Enumeration<URL> findResourcesFromLocal(String name) throws IOException{
        Enumeration<Resource> enumeration = resourceLoaderFactory.findAllResource(name);
        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return enumeration.hasMoreElements();
            }

            @Override
            public URL nextElement() {
                return enumeration.nextElement().getUrl();
            }
        };
    }

    private byte[] getClassByte(String formatClassName){
        InputStream inputStream = resourceLoaderFactory.getInputStream(formatClassName);
        if(inputStream == null){
            return null;
        }
        try {
            return IOUtils.read(inputStream);
        } catch (Exception e){
            log.error("Failed to read class bytes for: {}", formatClassName, e);
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String formatResourceName(String name) {
        return ResourceUtils.formatStandardName(name);
    }

    private String formatClassName(String className) {
        className = className.replace( '/', '~' );
        className = className.replace( '.', '/' ) + ".class";
        className = className.replace( '~', '/' );
        return className;
    }

}