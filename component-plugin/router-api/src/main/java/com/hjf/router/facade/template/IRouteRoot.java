package com.hjf.router.facade.template;

import android.content.Context;

import java.util.Map;

/**
 * Root element.
 * 手动创建实现类：RouteRootImpl
 * 1. apt 会在各组件生成多个 IRouterGroupImpl 类
 * 2. 在编译期借助 gradle 脚本进行字节码插入，将所有的 IRouterGroupImpl 类的添加代码插入到 IRouteRoot的loadInto()方法中
 * 3. 使用 {@link com.hjf.router.router.Router#init(Context)} 会执行
 *      RouteRootImpl#loadInto()，将个组件APT生成的 IRouteGroup 放入缓存
 */
public interface IRouteRoot {

    /**
     * Load routes to input
     *
     * @param routes input
     */
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}