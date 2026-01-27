package com.zqzqq.bootkits.bootstrap;

/**
 * SpringPluginBootstrap 实例绑定
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class SpringPluginBootstrapBinder {

    private final static ThreadLocal<SpringPluginBootstrap> BINDER = new ThreadLocal<>();


    public static SpringPluginBootstrap get(){
        return BINDER.get();
    }

    public static void set(SpringPluginBootstrap bootstrap){
        BINDER.set(bootstrap);
    }

    public static void remove(){
        BINDER.remove();
    }


}