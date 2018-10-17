package com.hjf.router.facade.template;

import com.hjf.router.facade.annotation.Route;
import com.hjf.router.facade.model.RouteMeta;

import java.util.Map;

/**
 * 1. apt 会在各组件生成 Router$$Group$$ModuleName 类，实现接口 {@link IRouteGroup}
 * .    在 ARouter 的设计中，就是方法 {@link IRouteGroup#loadInto(Map)}
 * .    职责： 将标记的各种类型{@link com.hjf.router.facade.enums.RouteType}的元素封装成 {@link RouteMeta} 对象并存放在仓库对象
 * .    {@link com.hjf.router.Warehouse#routes} 中
 * .    key值是注解中的 path 值，{@link Route#path()}
 * 2. 获取对象(Activity 、Fragment、IProvider...等)并使用过程类似{@link IProvider}
 * .    参考注解 4.2 根据 ByType 方式获取实例化对象，具体实现会根据type不同有所差异，但过程思路大体一致
 * 3. 初始化导入方式: 参考 {@link IRouteRoot} ： 2. 初始化导入方式一、二、三
 */
public interface IRouteGroup {
    /**
     * Fill the atlas with routes in group.
     */
    void loadInto(Map<String, RouteMeta> atlas);
}
