package com.hjf.router.facade.template;

import com.hjf.router.facade.annotation.Autowired;
import com.hjf.router.facade.service.AutowiredService;
import com.hjf.router.core.AutowiredServerImpl;
import com.hjf.router.router.Router;


/**
 * Autowired 功能帮助类 —— 注册器
 *
 * 1. 对于标注 {@link Autowired} 注解字段的 Class 会通过 APT 自动生成
 * .    ClassSimpleName$$Router$$Autowired 类，实现接口 {@link ISyringe}
 * .    APT 可以获取 当前ActivityName、标注字段的类型、{@link Autowired#name()}存放的key值
 * .    可以生成实现 {@link ISyringe#inject(Object)} 方法的代码：
 * .    activity1.num = activity1.getIntent().getIntExtra("autoWiredName", activity1.num)
 * .    可知： 自动填充的字段必须是 public 修饰的
 * 2. 如何调起 {@link Autowired} 实现类方法
 * .    在 Activity 初始化时 使用 {@link Router#inject(Object)}进行字段自动填充，跟踪代码流程：
 * .     - 利用 ARoute 获取了 AutowiredService 实现类 AutowiredServerImpl 的对象 -> autoServer
 * .     - 调用 autoServer.autowire(activity) 进入 AutowiredServerImpl.autowire(activity) 实现代码
 * .     - 根据 activity class name 获取取得 对应的  ISyringe 实现类
 * .     - 初始化获取 ISyringe 对象，即对应的 ClassSimpleName$$Router$$Autowired 实现类对象： iSyringe
 * .     - iSyringe.inject(activity) 进入 APT 生成的取值、赋值代码块
 */
public interface ISyringe {

    /**
     * 开始注入
     *
     * @param self the container itself, members to be inject into have been annotated
     *             with one annotation called Autowired
     */
    void inject(Object self);
}
