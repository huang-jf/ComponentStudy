package com.hjf.router;

import com.hjf.router.facade.model.RouteMeta;
import com.hjf.router.facade.template.IProvider;
import com.hjf.router.facade.template.IRouteGroup;

import java.util.HashMap;
import java.util.Map;

public class Warehouse {

    // Cache route:
    public static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    // Cache route and metas
    public static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    static Map<Class, IProvider> providers = new HashMap<>();
    public static Map<String, RouteMeta> providersIndex = new HashMap<>();


    static void clear() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
    }
}
