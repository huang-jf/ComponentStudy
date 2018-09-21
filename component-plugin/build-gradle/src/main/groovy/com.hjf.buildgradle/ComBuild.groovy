package com.hjf.buildgradle

import com.hjf.buildgradle.exten.ComExtension
import com.hjf.buildgradle.util.StringUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

class ComBuild implements Plugin<Project> {

    //默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease
    String compileModule = "app"

    void apply(Project project) {

        // 1. 读取应用此插件的 build.gradle 插件根目录的 combuild 属性
        project.extensions.create('combuild', ComExtension)

        // 2. 根据编译信息获取 task 对象
        // 2.1 获取 module 标识
        String module = project.path.replace(":", "")
        System.out.println("current module is " + module)
        // 2.2 获取此模块所有 task 名字，并生成对象
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        AssembleTask assembleTask = getTaskInfo(project.gradle.startParameter.taskNames)

        // 3. 匹配此次编译的模块，并赋值给 compileModule 字段
        if (assembleTask.isAssemble) {
            fetchMainModuleName(project, assembleTask)
            System.out.println("compilemodule  is " + compileModule)
        }

        // 4.1 获取 isRunAlone字段
        if (!project.hasProperty("isRunAlone")) {
            throw new RuntimeException("you should set isRunAlone in " + module + "'s gradle.properties")
        }

        // 4.2 动态修改 isRunAlone 的值，规则如下：
        //      isRunAlone = false，不用修改
        //      model是当前编译model，或是主项目，isRunAlone修改为true，其他组件都强制修改为false
        //      这就意味着组件不能引用主项目，这在层级结构里面也是这么规定的
        boolean isRunAlone = Boolean.parseBoolean((project.properties.get("isRunAlone")))
        String mainmodulename = project.rootProject.property("mainmodulename")
        if (isRunAlone && assembleTask.isAssemble) {
            if (module.equals(compileModule) || module.equals(mainmodulename)) {
                isRunAlone = true
            } else {
                isRunAlone = false
            }
        }
        project.setProperty("isRunAlone", isRunAlone)

        // 5. 根据配置添加各种组件依赖，并且自动化生成组件加载代码
        // 5.1 如果不是作为单独模块
        if (!isRunAlone) {
            project.apply plugin: 'com.android.library'
            // 不可能是壳App模块+
            // 排除用来单独开发的所有java代码
            /*project.android.sourceSets {
                main {
                    java {
                        exclude 'runalone/**'
                    }
                }
            }*/
            System.out.println("apply plugin is " + 'com.android.library')
        }
        // 5.2 最为单独模块
        else {
            project.apply plugin: 'com.android.application'
            // 不是壳App模块才进行如下处理
            if (!module.equals(mainmodulename)) {
                // 指向专门用来单独开发的 manifest
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/runalone/AndroidManifest.xml'
                    }
                }
            }
            System.out.println("apply plugin is " + 'com.android.application')
            // 自动添加依赖 - 当前模楷是编译模块时
            if (assembleTask.isAssemble && module.equals(compileModule)) {
                compileComponents(assembleTask, project)
                project.android.registerTransform(new ComCodeTransform(project))
            }
        }
    }

    private class AssembleTask {
        boolean isAssemble = false
        boolean isDebug = false
        List<String> modules = new ArrayList<>()
    }

    private AssembleTask getTaskInfo(List<String> taskNames) {
        AssembleTask assembleTask = new AssembleTask()
        for (String task : taskNames) {
            if (task.toUpperCase().contains("ASSEMBLE")
                    || task.contains("aR")
                    || task.contains("asR")
                    || task.contains("asD")
                    || task.toUpperCase().contains("TINKER")
                    || task.toUpperCase().contains("INSTALL")
                    || task.toUpperCase().contains("RESGUARD")) {
                if (task.toUpperCase().contains("DEBUG")) {
                    assembleTask.isDebug = true
                }
                assembleTask.isAssemble = true
                System.out.println("debug assembleTask info:" + task)
                String[] strs = task.split(":")
                assembleTask.modules.add(strs.length > 1 ? strs[strs.length - 2] : "all")
                break
            }
        }
        return assembleTask
    }

    /**
     * 根据当前的task，获取要运行的组件，规则如下：
     * assembleRelease ---app
     * app:assembleRelease :app:assembleRelease ---app
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     * @param assembleTask
     */
    private void fetchMainModuleName(Project project, AssembleTask assembleTask) {
        if (!project.rootProject.hasProperty("mainmodulename")) {
            throw new RuntimeException("you should set compilemodule in rootproject's gradle.properties")
        }
        if (assembleTask.modules.size() > 0 && assembleTask.modules.get(0) != null
                && assembleTask.modules.get(0).trim().length() > 0
                && !assembleTask.modules.get(0).equals("all")) {
            compileModule = assembleTask.modules.get(0)
        } else {
            compileModule = project.rootProject.property("mainmodulename")
        }
        if (compileModule == null || compileModule.trim().length() <= 0) {
            compileModule = "app"
        }
    }

    /**
     * 自动添加依赖，只在运行assemble任务的才会添加依赖，因此在开发期间组件之间是完全感知不到的，这是做到完全隔离的关键
     * 支持两种语法：module或者groupId:artifactId:version(@aar),前者之间引用module工程，后者使用maven中已经发布的aar
     * @param assembleTask
     * @param project
     */
    private void compileComponents(AssembleTask assembleTask, Project project) {
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
                /**
                 * 示例语法:groupId:artifactId:version(@aar)
                 * compileComponent=com.luojilab.reader:readercomponent:1.0.0
                 * 注意，前提是已经将组件aar文件发布到maven上，并配置了相应的repositories
                 */
                project.dependencies.add("api", str)
                System.out.println("add dependencies lib  : " + str)
            } else {
                /**
                 * 示例语法:module
                 * compileComponent=readercomponent,sharecomponent
                 */
                project.dependencies.add("api", project.project(':' + str))
                System.out.println("add dependencies project : " + str)
            }
        }
    }
}