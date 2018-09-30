# ComponentStudy

通过解剖分析[ARouter](https://github.com/alibaba/ARouter)源码，仿照编写代码，加深熟悉逻辑。

一下内容记录此项目仿照[ARouter](https://github.com/alibaba/ARouter)源码的实现思路和设计，便于日后复习和快速掌握。

## 实现功能

- 代码解耦合，模块组件完全隔离
- uri跳转，数据传递
- 自动判断组件是否作为application运行
- 代码隔离，面向接口开发，隐藏实现类



## 项目结构 TODO

- `com.hjf.router` 项目其实包名路径
  - `core`

## 项目功能接口、类

ARouter可以实现根据Uri获取各Fragment或其他的对象。

本质上是维护一个Map，根据字段匹配对象。

首先了解如何存放对象和其Key值到Map中

#### 各组件元素如何添加到仓库

#### Warehouse

用来存放各中Route元素的仓库。使用静态Map对象存放。

#### RouteRoot、IRouteGroup

1. APT 会在各组件Module生成多个 IRouterGroupImpl 类
2. 在编译期借助 gradle 脚本进行字节码插入，添加代码，类似：
   1. `new IRouterGroupImpl().loadInto(Warehouse.xxCacheMap)`
   2. 就实现了将组件的各class放入仓库中，使用时根据uri从仓库拿就可以了 
3. IRouterGroupImpl类有APT自动生成，实现了IRouteGroup接口。APT检索标注@Route的Activity，从注解中获取Key值，自身作为Value值，放入loadInto()传入的仓库map中。
4. 可根据种类不同：Activity、Fragment、Broadcast或是自定义的Class。实现不同的接口，生成不同的Java文件，可在编译期脚本传入不同的仓库map。

用到此相关逻辑的还有：

- IProviderGroup
- IProvider
- IInterceptor
- IInterceptorGroup

### 各组件元素如何从仓库获取



### 参数传递的实现思路

