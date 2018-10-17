package com.hjf.router;

import com.hjf.router.facade.model.RouteMeta;
import com.hjf.router.facade.template.IProvider;
import com.hjf.router.facade.template.IRouteGroup;

import java.util.HashMap;
import java.util.Map;

public class Warehouse {

    // Cache route: moduleName --> IRouteGroupImpl
    public static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    // Cache route and metas
    public static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    // 存放 IProvider 实现类对象，使用时才初始化对象并放入map
    public static Map<Class, IProvider> providers = new HashMap<>();
    public static Map<String, RouteMeta> providersIndex = new HashMap<>();


    static void clear() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
    }
}
