package com.hjf.router.facade.template;

import android.app.Application;

import com.hjf.router.facade.annotation.Route;
import com.hjf.router.facade.model.RouteMeta;

import java.util.Map;

/**
 * 1. apt 会在各组件生成 Router$$Group$$ModuleName 类，实现 {@link IRouteGroup} 接口的类
 * 2. 在编译期借助 gradle 脚本，IRouteRoot。将本接口所有的实现类存放如仓库的 {@link com.hjf.router.Warehouse#groupsIndex} 对象中
 * .    在 {@link Application#onCreate()} 时会调用实现类 loadInto 方法
 * 3. 此方法职能： 将所有标注 {@link com.hjf.router.facade.annotation.Route} 注解的 Activity 存放入仓库的
 * .     {@link com.hjf.router.Warehouse#routes} 对象中.
 * .    Key值： {@link Route#path()}
 * .    使用是根据 path 字符串来找
 * 4.
 */
public interface IRouteGroup {
    /**
     * Fill the atlas with routes in group.
     */
    void loadInto(Map<String, RouteMeta> atlas);
}
