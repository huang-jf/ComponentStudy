package com.hjf.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.hjf.plugin.util.ClassUtils
import javassist.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class RouterInitTransform extends Transform {

    /**
     * 用于判断当前 class 是否是 Application
     * 如果是： 需要插入代码
     */
    private static final String APPLICATION_NAME = "com.hjf.component.sample.MyApp"
    private static final String ROUTE_ROOT_NAME = "com.hjf.router.facade.template.IRouteRoot"

    private Project project
//    String applicationName

    RouterInitTransform(Project project) {
        this.project = project
    }

    // 设置我们自定义的Transform对应的Task名称
    @Override
    String getName() {
        return "RouterPluginInit"
    }

    // 指定输入的类型，通过这里的设定，可以指定我们要处理的文件类型这样确保其他类型的文件不会传入
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    // 指定Transform的作用范围
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        System.out.println("router init transform start")

        // 1. 获取应用程序的 Application Name TODO
//        applicationName = project.extensions.combuild.applicationName
//        if (applicationName == null || applicationName.isEmpty()) {
//            throw new RuntimeException("you should set applicationName in combuild")
//        }
//        System.out.println("1. applicationName ->  " + applicationName)

        // 1. 获取默认搜索路径类池
        System.out.println("1. 获取默认搜索路径类池 ")
        ClassPool classPool = ClassPool.getDefault()

        System.out.println("2. 加载 class 文件到 box 容器中 ")
        project.android.bootClasspath.each {
            // it：gradle中表示闭包的参数，类似this
            classPool.appendClassPath((String) it.absolutePath)
        }
        def box = ClassUtils.toCtClasses(transformInvocation.getInputs(), classPool)

        System.out.println("start find Target Application and All RouteRootImplClass.")
        CtClass ctClassApplication = null
        List<CtClass> routeRootList = new ArrayList<>()
        for (CtClass ctClass : box) {
            // 找到指定的 Application
            if (APPLICATION_NAME.equals(ctClass.getName())) {
                System.out.println("is application " + ctClass.getName())
                ctClassApplication = ctClass
                continue
            }
            // 找到所有的 RouteRoot 实现类
            if (isRouteRoot(ctClass)) {
                System.out.println("Add RouterRoot  Class -- " + ctClass.getName())
                routeRootList.add(ctClass)
            }
        }
        // 没有找到 Exception
        if (ctClassApplication == null) {
            System.out.println("Application class not found exception. class name -- " + APPLICATION_NAME)
            return
        }

        // Transform 的 inputs 有两种类型，一种是目录，一种是jar包，要分开遍历
        transformInvocation.inputs.each { TransformInput input ->

            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->

                // jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)
            }

            // 对类型为 目录(文件夹) 的input进行遍历, 开始自动注入
            input.directoryInputs.each { DirectoryInput directoryInput ->

                // 修改指定的 application.class
                System.out.println("start register dir path : " + directoryInput.file.getPath())
                String fileName = directoryInput.file.absolutePath
                File dir = new File(fileName)
                dir.eachFileRecurse { File file ->
                    String filePath = file.absolutePath
                    // 获取 class
                    String classNameTemp = filePath.replace(fileName, "")
                            .replace("\\", ".")
                            .replace("/", ".")
                    if (classNameTemp.endsWith(".class")) {
                        // 解析出 class name
                        String className = classNameTemp.substring(1, classNameTemp.length() - 6)
                        // 是指定的 Application 则插入代码
                        if (APPLICATION_NAME.equals(className)) {
                            System.out.println("update application class name is  : " + className)
                            insertInitializeCode2Application(ctClassApplication, routeRootList, classPool, fileName)
                            // 卸载，刷新pool里的代码
                            ctClassApplication.detach()
                        }
                    }
                }

                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
        classPool.clearImportedPackages()
        System.out.println("router init transform success.")
    }

    private
    static void insertInitializeCode2Application(CtClass ctClassApplication, List<CtClass> routeRootList, ClassPool classPool, String patch) {
        System.out.println("insert initialize code to application begin --> " + ctClassApplication.getName())
        ctClassApplication.defrost()

        try {
            // 获取 void onCreate() 方法
//            CtClass[] paramTypes = new CtClass[1]
//            paramTypes[0] = classPool.get(Void.class.getName())
            CtMethod methodOnCreate = ctClassApplication.getDeclaredMethod("onCreate", null)
            System.out.println("found onCreate() in Application, start insert initialize code ... ")
            // 开始插入代码
            methodOnCreate.insertBefore(generateInitializeCode4RouteRoots(routeRootList))
        }
        // 没有找到方法，自己创建
        catch (MissingMethodException | NotFoundException e) {
            System.out.println("could not found onCreate() in Application;   " + e.toString())
            System.out.println("try create method onCreate() in Target Application - " + ctClassApplication.getName())

            StringBuilder methodBody = new StringBuilder()
            methodBody.append("protected void onCreate() {")
            methodBody.append("super.onCreate();")
            // 循环添加Usher.input()代码
            methodBody.append(generateInitializeCode4RouteRoots(routeRootList))
            methodBody.append("}")
            ctClassApplication.addMethod(CtMethod.make(methodBody.toString(), ctClassApplication))
        }
        // 创建 onCreate 方法失败
        catch (Exception e) {
            System.out.println("Create method:onCreate failed in Application;   \n" + e.toString())
            return
        }
        ctClassApplication.writeFile(patch)
        System.out.println("insert initialize code 2 application success")
    }

    private static String generateInitializeCode4RouteRoots(List<CtClass> routeRootList) {
        StringBuilder initializeCodeBuilder = new StringBuilder()
        routeRootList?.forEach({
            initializeCodeBuilder
                    .append("new ")
                    .append(it.getName())
                    .append("().loadInto(com.hjf.router.Warehouse.groupsIndex);")
        })
        System.out.println(" initialize code : \n " + initializeCodeBuilder.toString())
        return initializeCodeBuilder.toString()
    }

    /**
     * 是否是 IRouteRoot 实现类
     *
     * 每个采用 APT： Route-Api-Compiler 的模块都会生成一个IRouteRoot的实现类，用来生成存放IRouteGroup、IProvider的生成类
     *
     * @return true
     */
    private boolean isRouteRoot(CtClass ctClass) {
        for (CtClass ctClassInter : ctClass.getInterfaces()) {
            if (ROUTE_ROOT_NAME.equals(ctClassInter.name)) {
                return true
            }
        }
        return false
    }

}