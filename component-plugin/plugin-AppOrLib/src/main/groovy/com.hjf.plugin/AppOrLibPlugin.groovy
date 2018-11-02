package com.hjf.plugin

import com.android.build.gradle.tasks.MergeManifests
import com.hjf.plugin.copy_from_manifesteditor.ManifestParser
import com.hjf.plugin.util.AppOrLibExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class AppOrLibPlugin implements Plugin<Project> {

    // 删除 application 冲突属性
    private static String[] UPDATE_MANIFEST_NODE_TASK_INFO_1 = [
            "application",
            "android:name=del&android:icon=del&android:label=del&android:roundIcon=del&android:theme=del"]
    // 删除 activity 默认启动配置：category、action 两个要留一个，空的 intent-filter 会报错
    private
    static String[] UPDATE_MANIFEST_NODE_TASK_INFO_2 = [
            "application/activity/intent-filter/category?android:name=android.intent.category.LAUNCHER&parent=1",
            "child:category=del"]

    private AppOrLibExtension appOrLibExtension

    @Override
    void apply(Project project) {

        // 1. 插件获取当前模块的 build.gradle 中获取参数
        appOrLibExtension = project.extensions.create("apporlib", AppOrLibExtension)

        // 2. 获取当前编译module
        String currCompileModuleName = project.getPath().replace(":", "")
        System.out.println("1. currCompileModuleName ->  " + currCompileModuleName)
        System.out.println("1. project.getPath() ->  " + project.getPath())

        // 3. 从工程根目录的 gradle.properties 文件中获取字段 main_module_name
        if (!project.rootProject.hasProperty("main_module_name")) {
            throw new RuntimeException("you should set main_module_name in " + module + "'s gradle.properties")
        }
        String mainModuleName = project.rootProject.property("main_module_name")
        System.out.println("2. mainModuleName ->  " + mainModuleName)

        // 4. 获取编译的 TargetCompileModuleName
        // 如果是命令行使用 ./gradlew compileDebug 等进行编译，TargetCompileModuleName = null
        String targetCompileModuleName = getTargetCompileModuleName(project)
        if (targetCompileModuleName == null) {
            System.out.println("3. 检测 targetCompileModuleName = null, 默认为 application")
//            targetCompileModuleName = mainModuleName
        }
        System.out.println("3. targetCompileModuleName ->  " + targetCompileModuleName)

        // 5. 判断当前module应用插件：application、library
        boolean isApplyPluginApplication = true
        if (targetCompileModuleName != null
                && !currCompileModuleName.equals(mainModuleName)
                && !currCompileModuleName.equals(targetCompileModuleName)) {
            isApplyPluginApplication = false
        }
        System.out.println("4. isApplyPluginApplication ->  " + isApplyPluginApplication)

        // 选择使用插件
        // 6.1 application
        // 根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isApplyPluginApplication) {
            System.out.println("apply plugin is " + 'com.android.application')
            project.apply plugin: 'com.android.application'
        }
        // 6.2 library
        // 排除不要的java代码等操作
        // 移除 manifest 节点 application 无需元素，如：name，或是 默认启动Activity
        else {
            System.out.println("apply plugin is " + 'com.android.library')
            project.apply plugin: 'com.android.library'
        }

        // 7. 获取 build.gradle 自定义参数只有在此方法块中才能使用
        project.afterEvaluate {

            // 6.1 编译时期: 自动添加引用
            if (isCompileTask(project)) {
                appOrLibExtension.compileLibs.each { libName ->
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
            // 6.2 添加引用后
            if (isApplyPluginApplication) {
                delManifestNode(project)
            }
        }
    }

    /**
     * 删除所有依赖包的 manifest 冲突标签属性，比如：
     * -    application->android:name ...
     * -    Activity-> 默认启动标签：LAUNCHER
     */
    private static void delManifestNode(Project project) {
        project.android.applicationVariants.all { variant ->
            variant.outputs.each { output ->
                // 在本 Module 的 Manifest 合并之前，检查所有依赖包的 Manifest 文件，进行需要的动作
                MergeManifests processManifestTask = output.processManifest
                processManifestTask.doFirst {
                    def manifestFiles = processManifestTask.manifests.files
                    for (manifestFile in manifestFiles) {
                        System.out.println("Edit File  --  ${manifestFile.getAbsolutePath()} Start.")

                        ManifestParser manifestParser = new ManifestParser(manifestFile)
                        manifestParser.editNode(UPDATE_MANIFEST_NODE_TASK_INFO_1[0], UPDATE_MANIFEST_NODE_TASK_INFO_1[1])
                        manifestParser.editNode(UPDATE_MANIFEST_NODE_TASK_INFO_2[0], UPDATE_MANIFEST_NODE_TASK_INFO_2[1])
                        manifestParser.save()

                        System.out.println("Edit File  --  ${manifestFile.getAbsolutePath()}  OK.")
                    }
                }

                // 在AS合并 manifest 之后修改 manifest 的内容，保证不被AS重新覆写
                output.processManifest.doLast {
                    // nothing ...
                }
            }
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
     * 默认是app，直接运行assembleRelease的时候，等同于运行 app:assembleRelease
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
}