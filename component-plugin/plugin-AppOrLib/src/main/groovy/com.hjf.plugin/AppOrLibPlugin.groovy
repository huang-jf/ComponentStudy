package com.hjf.plugin


import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class AppOrLibPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        // 1. 获取当前编译module
        String currCompileModuleName = project.getPath().replace(":", "")
        System.out.println("1. currCompileModuleName ->  " + currCompileModuleName)
        System.out.println("1. project.getPath() ->  " + project.getPath())

        // 2. 从工程根目录的 gradle.properties 文件中获取字段 main_module_name
        if (!project.rootProject.hasProperty("main_module_name")) {
            throw new RuntimeException("you should set main_module_name in " + module + "'s gradle.properties")
        }
        String mainModuleName = project.rootProject.property("main_module_name")
        System.out.println("2. mainModuleName ->  " + mainModuleName)

        // 3. 获取编译的 TargetCompileModuleName
        // 如果是命令行使用 ./gradlew compileDebug 等进行编译，TargetCompileModuleName = null
        String targetCompileModuleName = getTargetCompileModuleName(project)
        if (targetCompileModuleName == null) {
            System.out.println("3. 检测 targetCompileModuleName = null, 默认为 application")
//            targetCompileModuleName = mainModuleName
        }
        System.out.println("3. targetCompileModuleName ->  " + targetCompileModuleName)

        // 4. 判断当前module应用插件：application、library
        boolean isApplyPluginApplication = true
        if (targetCompileModuleName != null
                && !currCompileModuleName.equals(mainModuleName)
                && !currCompileModuleName.equals(targetCompileModuleName)) {
            isApplyPluginApplication = false
        }
        System.out.println("4. isApplyPluginApplication ->  " + isApplyPluginApplication)

        // 5.1 application
        // 根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isApplyPluginApplication) {
            System.out.println("apply plugin is " + 'com.android.application')
            project.apply plugin: 'com.android.application'

            // 更改 manifest 位置
            String manifestsSrcFileAsApplication = project.properties.get("manifestsSrcFileAsApplication")
            System.out.println("manifestsSrcFileAsApplication --> " + manifestsSrcFileAsApplication)
            if (manifestsSrcFileAsApplication != null) {
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/asapp/AndroidManifest.xml'
                    }
                }
            }

            // 编译时期: 自动添加引用
            if (isCompileTask(project)) {
                String compileLibs = (String) project.properties.get("compileLibs")
                System.out.println("compileLibs --> " + compileLibs)
                if (null != compileLibs) {
                    String[] compileLibArray = compileLibs.split(",")
                    for (String libName : compileLibArray) {
                        libName = libName.trim()
                        if (libName.startsWith(":")) {
                            libName = libName.substring(1)
                        }
                        // maven 座标导入: api 'com.android.support:appcompat-v7:26.1.0'
                        if (isMavenArtifact(libName)) {
                            System.out.println("add dependencies lib  : " + libName)
                            project.dependencies.add("api", libName)
                        }
                        // 本地项目导入: api project(':module_XXX')
                        else {
                            System.out.println("add dependencies lib : " + libName)
                            project.dependencies.add("api", project.project(':' + libName))
                        }
                    }
                }
            }
        }
        // 5.2 library
        // 排除不要的java代码等操作
        else {
            System.out.println("apply plugin is " + 'com.android.library')
            project.apply plugin: 'com.android.library'
            // java file 排除设置 FIXME 目前无效
            // runalone/**,runalone2/xx.java
            String excludeJavaFiles = (String) project.properties.get("excludeJavaFilePathsAsLibrary")
            System.out.println("excludeJavaFiles --> " + excludeJavaFiles)
            /*if (excludeJavaFiles != null) {
                String[] excludeJavaFileArray = compileLibs.split(",")
                project.android.sourceSets {
                    main {
                        java {
                            exclude excludeJavaFileArray
                        }
                    }
                }
            }*/
        }
    }

    /**
     * 是否是maven 坐标
     */
    private static boolean isMavenArtifact(String str) {
        if (str == null || str.isEmpty()) {
            return false
        }
        return Pattern.matches("\\S+(\\.\\S+)+:\\S+(:\\S+)?(@\\S+)?", str)
    }
    /**
     * 获取目标编译组件的名字，规则如下：
     * 默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease
     *
     * assembleRelease ---app
     * app:assembleRelease :app:assembleRelease ---app
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     * @param assembleTask
     */
    private static String getTargetCompileModuleName(Project project) {
        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames()
        for (String taskName : taskNames) {
            if (taskName.contains("aR")
                    || taskName.contains("asR")
                    || taskName.contains("asD")
                    || taskName.toUpperCase().contains("ASSEMBLE")
                    || taskName.toUpperCase().contains("TINKER")
                    || taskName.toUpperCase().contains("INSTALL")
                    || taskName.toUpperCase().contains("RESGUARD")) {

                System.out.println("3. taskName ->  " + taskName)
                String[] strs = taskName.split(":")
                return strs.length > 1 ? strs[strs.length - 2] : "all"
            }
        }
        return null
    }

    /**
     * 是否时编译任务
     * 因为在开发状态下，App，ModuleA都是Application，是不能互相建立引用关系的
     * 需要在编译时期，本插件自动更改application为library并添加引用
     */
    private static boolean isCompileTask(Project project) {
        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames()
        for (String taskName : taskNames) {
            if (taskName.contains("aR")
                    || taskName.contains("asR")
                    || taskName.contains("asD")
                    || taskName.toUpperCase().contains("ASSEMBLE")
                    || taskName.toUpperCase().contains("TINKER")
                    || taskName.toUpperCase().contains("INSTALL")
                    || taskName.toUpperCase().contains("RESGUARD")) {
//                boolean isDebug = taskName.toUpperCase().contains("DEBUG");
                return true
            }
        }
        return false
    }

    /**
     * 自动添加依赖，只在运行assemble任务的才会添加依赖，因此在开发期间组件之间是完全感知不到的，这是做到完全隔离的关键
     * 支持两种语法：module或者groupId:artifactId:version(@aar),前者之间引用module工程，后者使用maven中已经发布的aar
     * @param assembleTask
     * @param project
     */
    /*private void compileComponents(AssembleTask assembleTask, Project project) {
        // 1. 获取当前模块的 gradle.properties 文件申明的 debugComponent、compileComponent 字段
        String components
        if (assembleTask.isDebug) {
            components = (String) project.properties.get("debugComponent")
        } else {
            components = (String) project.properties.get("compileComponent")
        }

        if (components == null || components.length() == 0) {
            System.out.println("there is no add dependencies ")
            return
        }

        // 2. 解析成依赖模块数组
        String[] compileComponents = components.split(",")
        if (compileComponents == null || compileComponents.length == 0) {
            System.out.println("there is no add dependencies ")
            return
        }

        // 3. 循环导入
        for (String str : compileComponents) {
            System.out.println("comp is " + str)
            str = str.trim()
            if (str.startsWith(":")) {
                str = str.substring(1)
            }
            // 是否是maven 坐标
            if (StringUtil.isMavenArtifact(str)) {
                *//**
     * 示例语法:groupId:artifactId:version(@aar)
     * compileComponent=com.luojilab.reader:readercomponent:1.0.0
     * 注意，前提是已经将组件aar文件发布到maven上，并配置了相应的repositories
     *//*
                project.dependencies.add("api", str)
                System.out.println("add dependencies lib  : " + str)
            } else {
                *//**
     * 示例语法:module
     * compileComponent=readercomponent,sharecomponent
     *//*
                project.dependencies.add("api", project.project(':' + str))
                System.out.println("add dependencies project : " + str)
            }
        }
    }*/
}