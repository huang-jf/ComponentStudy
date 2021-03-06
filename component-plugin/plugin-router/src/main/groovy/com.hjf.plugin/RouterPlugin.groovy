package com.hjf.plugin

import com.hjf.plugin.RouterInitTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 遍历所有的class，进行字节码插入操作
 * 将标记：Route的class进行初始化操作，并放入map中缓存
 */
class RouterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        System.out.println("Router Plugin Start Transform --> init transform.")
        project.android.registerTransform(new RouterInitTransform(project))
    }

}