package com.hjf.router.facade.template;

import java.util.Map;

/**
 * Root element.
 * 1. apt 会在各组件生成 Router$$Root$$ModuleName 类，实现接口 {@link IRouteRoot}
 * .    将 APT 自动生成的 Router$$Group$$ModuleName 类存放入仓库对象
 * .    {@link com.hjf.router.Warehouse#groupsIndex} 中
 * .    key值是 APT 获取模块的 build.gradle 中申明的类似 ModuleName 的字段
 * 2.3 初始化导入方式一: gradle 插件字节码插入 - LogisticsCenter
 * .    ARouter的做法，在 LogisticsCenter.loadRouterMap() 中插入代码：
 * .        registerRouteRoot(new Router$$Root$$ModuleName());
 * .    在调用 ARouter.init() 后，最调用方法 LogisticsCenter.loadRouterMap()
 * .    注：在改方法内将字段赋值： registerByPlugin = true，这样就不会使用方式二去初始化
 * 2.2 初始化导入方式二: 读取Dex包
 * .    从 dex 包中读取所有文件名，筛选符合条件的文件名
 * .    使用 {@link Class} 工具实例化对象并调用导入方法
 * .    因为涉及文件读取等耗时操作，文件数过多，dex包过多，会拖慢App启动时间，建议用方式一
 * 2.3 初始化导入方式三: gradle 插件字节码插入 - Application
 * .    注： 没有全部采用 ARouter 的所有代码，主要目的是验证功能是否正常实现
 * .    gradle 插件在编译期扫描所有的 class 文件
 * .    找到项目 application 文件
 * .    找到所有的 Router$$Root$$ModuleName 类
 * .    在 application.onCreate() 方法中遍历插入类似： Router$$Root$$ModuleName.loadInto(map) 的方法调用代码
 * 3. {@link IRouteRoot} 多这一层的意义
 * .    技术上可以直接在 gradle 编译期插入 {@link IRouteGroup#loadInto(Map)} 导入代码
 * .    对一层 IRouteRoot 是实现了模块资源使用时加载功能
 * .    在编译期借助 Router$$Root$$ModuleName 实现类存入收集所有的 {@link IRouteGroup} 实现类
 * .    key 值是 ModuleName，可以在使用指定木块时加载模块配置到缓存中
 */
public interface IRouteRoot {

    /**
     * Load routes to input
     *
     * @param routes input
     */
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}