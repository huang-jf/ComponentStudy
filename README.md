# ComponentStudy

通过解剖分析[ARouter](https://github.com/alibaba/ARouter)源码，仿照编写代码，深入了解实现逻辑。



## 实现功能

- 代码解耦合，模块组件完全隔离
- uri跳转，数据传递
- 自动判断组件是否作为application运行
- 代码隔离，面向接口开发，隐藏实现类



## 项目功能接口、类

ARouter可以实现根据Uri获取各Fragment或其他的对象。

本质上是维护一个Map，根据字段匹配对象。

首先了解如何存放对象和其Key值到Map中

#### 各组件元素如何添加到仓库

#### Warehouse

用来存放各中Route元素的仓库。使用静态Map对象存放。     

#### IRouteRoot

- apt 会在各组件生成 `Router$$Root$$ModuleName` 类，实现接口 `{@link IRouteRoot}`
  - 将 APT 自动生成的 `Router$$Group$$ModuleName` 类存放入仓库对象
  - `{@link com.hjf.router.Warehouse#groupsIndex}` 中
  -  key值是 APT 获取模块的 build.gradle 中申明的类似 ModuleName 的字段
- 初始化导入方式一: gradle 插件字节码插入 - `LogisticsCenter`
  - gradle 插件在编译期扫描所有的 class 文件
  - 找到项目 LogisticsCenter 文件
  - 找到所有的 `Router$$Root$$ModuleName `类
  - 在 `LogisticsCenter.loadRouterMap()` 方法中遍历插入类似代码，实现导入。
  - `registerRouteRoot(new Router$$Root$$ModuleName());`
  - 同时需要插入此句代码：
  - `registerByPlugin = true`
  - 表示用方式一初始化，不去用调用方式二
- 初始化导入方式二: 读取Dex包
  - 从 dex 包中读取所有文件名，筛选符合条件的文件名
  - 使用 `{@link Class}` 工具实例化对象并调用导入方法
  - 因为涉及文件读取等耗时操作，文件数过多，dex包过多，会拖慢App启动时间
- 初始化导入方式三:  初始化导入方式三:  gradle 插件字节码插入 - Application
  - **注： 没有全部采用 ARouter 的所有代码，主要目的是验证功能是否正常实现**
  - gradle 插件在编译期扫描所有的 class 文件
  - 找到项目 application 文件
  - 找到所有的 `Router$$Root$$ModuleName` 类
  - 在 application.onCreate() 方法中遍历插入类似：
  - `Router$$Root$$ModuleName.loadInto(map)` 的方法调用代码
-  `{@link IRouteRoot}` 多这一层的意义
  - 技术上可以直接在 gradle 编译期插入 `{@link IRouteGroup#loadInto(Map)}` 导入代码
  - 对一层 IRouteRoot 是实现了模块资源使用时加载功能
  - 在编译期借助 `Router$$Root$$ModuleName` 实现类存入收集所有的 `{@link IRouteGroup} `实现类
  - key 值是 ModuleName，可以在使用指定木块时加载模块配置到缓存中


#### IRouteGroup

- apt 会在各组件生成 `Router$$Group$$ModuleName` 类，实现接口 {@link IRouteGroup}
  - 在 ARouter 的设计中，就是方法 `{@link IRouteGroup#loadInto(Map)}`
  - 职责： 将标记的各种类型`RouteType`的元素封装成`RouteMeta` 对象并存放在仓库对象
  - `{@link com.hjf.router.Warehouse#routes}` 中
  - key值是注解中的 path 值，`{@link Route#path()}`
- 获取对象(`Activity` 、`Fragment`、`IProvider`...等)并使用过程类似**IProvider**的获取方式
  - 参考 **IProvider**根据 ByType 方式获取实例化对象
  - 具体实现会根据type不同有所差异，但过程思路大体一致
- 初始化导入方式: 参考 **IRouteRoot**： 2. 初始化导入方式一、二、三

#### IProvider

- apt 会在各组件生成 `Router$$Providers$$ModuleName` 类，实现接口 `{@link IProviderGroup}`
  - 同时也会生成 `Router$$Group$$ModuleName` 类，实现接口`{@link IRouteGroup}`
  - 之所以有两个文件是因为 ARouter 提供 **ByName**、**ByType** 两种方式获取 Provider 对象
- ByName 方式获取 Provider： `ARouter.getInstance().navigation(MyService.class);`
  - 对应 APT 生成文件名是： `Router$$Providers$$ModuleName`
  - 实现的 `{@link IProviderGroup#loadInto(Map)}` 方法会将 Provider 封装成 RouteMate 对象存放入仓库对象
  - `{@link com.hjf.router.Warehouse#providersIndex}` 中
  - `key值是接口全路径： com.hjf.arouterdemo.MyService，注意是接口名不是实现类的全路径`
  - 意味着： 在 **ByName** 方式下，意味着**一个接口只能有一个实现类**能被获取到，其他的会被最后一个覆盖掉
- ByType 方式获取 Provider： `(MyService) ARouter.getInstance().build("/service/hello").navigation();`
  - 对应 APT 生成文件名是： `Router$$Group$$ModuleName` —— 就是存放 Activity RouteMate 元素的那个
  - 实现的 `{@link IRouteGroup#loadInto(Map)}` 方法会将 Provider 封装成 RouteMate 对象存放入仓库对象
  - `{@link com.hjf.router.Warehouse#routes}` 中 —— 就是存放 Activity RouteMate 元素的那个
  - 逻辑、过程和 **IRouteGroup** 讲解中的一致
  - key值是注解中的 path 值，只要 path 值不同，可以获取同一接口的不同实现类
- Provider 对像只有在获取的时候才会实例化并放入仓库对象
  -  `{@link com.hjf.router.Warehouse#providers}` 中缓存
  - 实例化过程分为两种： ByName、ByType
  - 实例化——ByName
    - 根据接口全路径从仓库 `{@link Warehouse#providersIndex} `获取 RouteMate 元素
    - 根据 `{@link RouteMeta#destination}` 对象进行实例化并返回
  - 实例化——ByType
    - 根据path字符串从仓库 `{@link Warehouse#routes} `获取 RouteMate 元素
    - 判断 `{@link RouteMeta#type}` 为 Provider 时实例化对象并返回对象
- 初始化导入方式: 参考 **IRouteRoot**： 2. 初始化导入方式一、二、三

## 参数传递的实现思路

在 ARouter 框架的设计实现中的相关关系：

- `AutowiredServerImpl`是接口`AutowiredService`的实现类
- `ISyringe`接口是 APT 自动生成 `ClassName$$Router$$Autowired`类要实现的接口
- `AutowiredServerImpl.autowire(Object instance)`方法实现中，通过传入的 `instance`对象的**ClassName**属性找到对应的`ClassName$$Router$$Autowired`，实例化其对象并调用其实现`ISyringe`接口的`inject`方法

#### AutowiredService

接口

#### AutowiredServerImpl

接口`AutowiredService`的实现类，使用 Route 注解，调用时借助 `ARoute` 获取。在 `Acvitity` 初始化时调用

`Router.getInstance().inject(activity);`进行字段自动填充，跟踪代码流程：

1. 利用 `ARoute` 获取了 `AutowiredService` 实现类 `AutowiredServerImpl` 的对象：**autoServer**

2. 调用 `autoServer.autowire(activity)` 进入 `AutowiredServerImpl.autowire(activity)` 实现代码

3. 根据 activity class name 获取取得 对应的  `ISyringe` 实现类

4. 初始化获取 `ISyringe` 对象，即对应的 `ClassSimpleName$$Router$$Autowired`  实现类对象： `iSyringe`

5. `iSyringe.inject(activity)` 进入 APT 生成的取值、赋值代码块

   

#### ISyringe

- 对于标注 `{@link Autowired}` 注解字段的 Class 会通过 APT 自动生成
- `ClassSimpleName$$Router$$Autowired` 类，实现接口 `{@link ISyringe}`
- APT 可以获取 `当前ActivityName`、`标注字段的类型`、`{@link Autowired#name()}`存放的key值
- 可以生成实现 `{@link ISyringe#inject(Object)}` 方法的代码：
  - `activity1.num = activity1.getIntent().getIntExtra("autoWiredName", activity1.num)`
- 有此段代码可知： 自动填充的字段`activity1.num`必须是 **public** 修饰的



上述均已 Activity 为例，Fragment等思路相同，具体实现代码需要做些处理。



## 组件化中使用AOP功能

- 需要在编译的模块使用`AspectjPlugin`插件
- 在主体项目中也要使用此`AspectjPlugin`插件

### AspectjPlugin 插件

- 集成了 `Aspectj` `gradle` 配置代码
- 自动区分当前模块种类，使用不同的`Aspectj`配置方式
  - `com.android.application`
  - `com.android.library`