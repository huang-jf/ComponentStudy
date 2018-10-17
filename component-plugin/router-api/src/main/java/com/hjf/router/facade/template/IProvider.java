package com.hjf.router.facade.template;


import android.content.Context;

import java.util.Map;

/**
 * Provider element.
 * 1. apt 会在各组件生成 Router$$Providers$$ModuleName 类，实现接口 {@link IProviderGroup}
 * .    同时也会生成 Router$$Group$$ModuleName 类，实现接口 {@link IRouteGroup}
 * .    之所以有两个文件是因为 ARouter 提供 ByName、ByType 两种方式获取 Provider 对象
 * 2. ByName 方式获取 Provider： ARouter.getInstance().navigation(HelloService.class);
 * .    对应 APT 生成文件名是： Router$$Providers$$ModuleName
 * .    实现的 {@link IProviderGroup#loadInto(Map)} 方法会将 Provider 封装成 RouteMate 对象存放入仓库对象
 * .    {@link com.hjf.router.Warehouse#providersIndex} 中
 * .    key值是接口全路径： com.hjf.arouterdemo.MyService，注意是接口名不是实现类的全路径
 * .      意味着： 在 ByName 方式下，意味着一个接口只能有一个实现类能被获取到，其他的会被最后一个覆盖掉
 * 3. ByType 方式获取 Provider： (HelloService) ARouter.getInstance().build("/service/hello").navigation();
 * .    对应 APT 生成文件名是： Router$$Group$$ModuleName —— 就是存放 Activity RouteMate 元素的那个
 * .    实现的 {@link IRouteGroup#loadInto(Map)} 方法会将 Provider 封装成 RouteMate 对象存放入仓库对象
 * .    {@link com.hjf.router.Warehouse#routes} 中 —— 就是存放 Activity RouteMate 元素的那个
 * .    逻辑、过程和 {@link IRouteGroup} 讲解中的一致
 * .    key值是注解中的 path 值，只要 path 值不同，可以获取同一接口的不同实现类。
 * 4. Provider 对像只有在获取的时候才会实例化并放入仓库对象
 * .    {@link com.hjf.router.Warehouse#providers} 中缓存
 * .    实例化过程分为两种： ByName、ByType
 * 4.1. 实例化——ByName
 * .    -   根据接口全路径从仓库 {@link com.hjf.router.Warehouse#providersIndex} 获取 RouteMate 元素
 * .    -   根据 {@link com.hjf.router.facade.model.RouteMeta#destination} 对象进行实例化并返回
 * 4.2. 实例化——ByType
 * .    -   根据path字符串从仓库 {@link com.hjf.router.Warehouse#routes} 获取 RouteMate 元素
 * .    -   判断 {@link com.hjf.router.facade.model.RouteMeta#type} 为 Provider 时实例化对象并返回
 * 5.1 初始化导入方式: 参考 {@link IRouteRoot} ： 2. 初始化导入方式一、二、三
 */
public interface IProvider {

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    void init(Context context);
}
