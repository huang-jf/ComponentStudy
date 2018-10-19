package com.hjf.plugin

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @使用ajc编译java代码 ， 同 时 织 入 切 片 代 码
 * 使用 AspectJ 的编译器（ajc，一个java编译器的扩展）
 * 对所有受 aspect 影响的类进行织入。
 * 在 gradle 的编译 task 中增加额外配置，使之能正确编译运行。
 *
 * 需要在编译的模块使用此插件
 * 在主体项目中也要使用此插件
 */
public class AspectjPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.dependencies {
            implementation 'org.aspectj:aspectjrt:1.9.1'
        }

        System.out.println("========================")
        System.out.println("Aspectj切片开始编织Class!")
        System.out.println("========================")

        // 使用此插件的模块是： android.application
        // 使用 project.android.applicationVariants 字段
        if (project.getPluginManager().hasPlugin("com.android.application")){
            project.android.applicationVariants.all { variant ->
                configAspectj(variant.javaCompile, project.logger)
            }
        }
        // 使用此插件的模块是： android.library
        // 使用 project.android.libraryVariants 字段
        else if (project.getPluginManager().hasPlugin("com.android.library")){
            project.android.libraryVariants.all { variant ->
                configAspectj(variant.javaCompile, project.logger)
            }
        }
    }

    private static void configAspectj(def javaCompile, final def log){

        javaCompile.doLast {
            String[] args = ["-showWeaveInfo",
                             "-1.8",
                             "-inpath", javaCompile.destinationDir.toString(),
                             "-aspectpath", javaCompile.classpath.asPath,
                             "-d", javaCompile.destinationDir.toString(),
                             "-classpath", javaCompile.classpath.asPath,
                             "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]

//            System.out.println("ajc args: " + Arrays.toString(args))

            MessageHandler handler = new MessageHandler(true)
            new Main().run(args, handler);
            for (IMessage message : handler.getMessages(null, true)) {
                switch (message.getKind()) {
                    case IMessage.ABORT:
                    case IMessage.ERROR:
                    case IMessage.FAIL:
                        log.error message.message, message.thrown
                        break;
                    case IMessage.WARNING:
                        log.warn message.message, message.thrown
                        break;
                    case IMessage.INFO:
                        log.info message.message, message.thrown
                        break;
                    case IMessage.DEBUG:
                        log.debug message.message, message.thrown
                        break;
                }
            }
        }
    }
}