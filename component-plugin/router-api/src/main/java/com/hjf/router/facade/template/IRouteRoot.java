package com.hjf.router.facade.template;

import android.app.Application;
import android.content.Context;

import java.util.Map;

/**
 * Root element.
 * 1. apt 会在各组件生成 Router$$Root$$ModuleName 类，实现 {@link IRouteRoot} 接口的类
 * 2. 在编译期借助 gradle 插件读取或有实现 {@link IRouteRoot} 接口的类，即 APT 自动生成的 Router$$Root$$ModuleName
 * 3. 遍历所有实现类，在 {@link Application#onCreate()} 代码块中插入 {@link IRouteRoot#loadInto(Map)} 方法的调用代码。
 * .   此方法具体实现看compiler包，主要工作是：
 * .   将当前模块 APT 自动生成的 Router$$Group$$ModuleName 实现 {@link IRouteGroup} 的类
 * .   放入 map 中 {@link com.hjf.router.Warehouse#groupsIndex} 中存储
 * <p>
 * <p>
 * 注： 下一步操作是 实例化 Router$$Group$$ModuleName 对象并调用其方法，相关本类的操作到此结束。
 * <p>
 * 注： 上述流程为简便已RootGroup为例，同理： ARouter 中 IProvider、IInterceptor 都此添加逻辑
 * .    在 IRouteRoot 实现类的方法中，将自动生成的 IProvider、IInterceptor 实现类放入
 * .    仓库类 {@link com.hjf.router.Warehouse} 对应的 map 静态对象中
 */
public interface IRouteRoot {

    /**
     * Load routes to input
     *
     * @param routes input
     */
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}