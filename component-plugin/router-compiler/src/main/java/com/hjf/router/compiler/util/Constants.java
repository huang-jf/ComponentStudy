package com.hjf.router.compiler.util;

public interface Constants {


    ///////////////////////////////////////////////////////////////////////////
    // Options of processor
    ///////////////////////////////////////////////////////////////////////////
    String OPTION_MODULE_NAME = "module_name";

    // System interface
    String ACTIVITY = "android.app.Activity";
    String FRAGMENT = "android.app.Fragment";
    String FRAGMENT_V4 = "android.support.v4.app.Fragment";
    String SERVICE = "android.app.Service";
    String BUNDLE = "android.os.Bundle";

    String ROUTE_META = "com.hjf.router.facade.model.RouteMeta";
    String ROUTE_TYPE = "com.hjf.router.facade.enums.RouteType";
    String ROUTE_ROOT = "com.hjf.router.facade.template.IRouteRoot";
    String ROUTE_GROUP = "com.hjf.router.facade.template.IRouteGroup";
    String ROUTE_SYRINGE = "com.hjf.router.facade.template.ISyringe";


    String MISSING_BASE_COMP_ROUTER_MSG = "interface ModuleClient not found\n确认以下细节：" +
            "1.该Module是否使用了componentlib依赖\n" +
            "2.确保版本适配，最新的适配版本套件，参考仓库release-note\n" +
            "3.使用的componentlib库中 ModuleClient 没有被混淆\n" +
            "more detail: https://github.com/mqzhangw/JIMU/issues/26";
}
