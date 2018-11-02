package com.hjf.component.sample;


import com.hjf.module.core.BaseApplication;
import com.hjf.router.Warehouse;
import com.hjf.router.facade.template.IRouteGroup;

import java.util.Map;

public class MyApp extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // gradle 使用 javassist 框架进行代码插入
        // 主要实现功能：将所有的 Router$$Root$$test 实现类调用 loadInto 方法
//        new Router$$Root$$test().loadInto(Warehouse.groupsIndex);

        initLoadRouteGroup();
    }

    /**
     * 注册RoteGroup、IProvider 的是 RouteRoot，借助Gradle编译时期字节码插入
     *
     * 加载注册的所有 Route Group 对象
     *
     * ARouter 的代码添加在 LogisticsCenter.loadRouterMap() 方法中
     */
    private void initLoadRouteGroup() {
        // java 代码
        for (Map.Entry<String, Class<? extends IRouteGroup>> next : Warehouse.groupsIndex.entrySet()) {
//            String moduleName = next.getKey();
            Class<? extends IRouteGroup> value = next.getValue();
            try {
                value.newInstance().loadInto(Warehouse.routes);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
