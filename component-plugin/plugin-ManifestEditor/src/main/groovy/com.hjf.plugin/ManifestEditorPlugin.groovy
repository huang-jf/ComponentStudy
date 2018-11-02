package com.hjf.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.hjf.plugin.util.ManifestEditExtension
import com.hjf.plugin.util.ManifestParser
import com.hjf.plugin.util.Utils
import groovy.xml.Namespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * Created by 2bab
 */
class ManifestEditorPlugin implements Plugin<Project> {


    private ManifestEditExtension editExtension
    public HashMap<String, Namespace> xmlNameSpaceMap = new HashMap<>()

    void apply(Project project) {
        // 1. 插件获取当前模块的 build.gradle 中获取参数
        editExtension = project.extensions.create("manifestEdit", ManifestEditExtension)

        project.afterEvaluate {
            // 需要在 project.afterEvaluate 中才能获取到
            System.out.println("create ManifestEditExtension enabled: ${project.manifestEdit.enabled}")
            System.out.println("create ManifestEditExtension editNodes: ${editExtension.editNodes}")

            // 不进行 manifest 修改操作
            if (!project.manifestEdit.enabled) {
                System.out.println("ManifestEditor Plugin enabled=false，不处理")
                return
            }

            // application
            if (project.getPlugins().hasPlugin(AppPlugin.class)) {
                project.android.applicationVariants.all { variant ->
                    addHandleAction(variant)
                }
            }
            // library
            else if (project.getPlugins().hasPlugin(LibraryPlugin.class)) {
                project.android.libraryVariants.all { variant ->
                    addHandleAction(variant)
                }
            }
            // 其他不支持
            else {
                System.out.println("ManifestEditor： 不支持此模块")
            }
        }
    }

    /**
     * 1. 添加插入处理动作，在当前模块合并 Manifest 之前、之后插入Action
     *  -   寻找到指定的 Manifest.xml 文件
     * @param variant
     */
    private void addHandleAction(def variant) {
        variant.outputs.each { output ->

            // 在本 Module 的 Manifest 合并之前，检查所有依赖包的 Manifest 文件，进行需要的动作
            output.processManifest.doFirst {

            }

            // 在AS合并 manifest 之后修改 manifest 的内容，保证不被AS重新覆写
            output.processManifest.doLast {
                // 获取编译manifest后的相关文件
                // 可能是文件也可能是文件夹，进行遍历检查
                FileCollection fileCollection = output.processManifest.outputs.getFiles()
                fileCollection.getFiles().each { file ->
                    findManifest(file)
                }
            }
        }
    }

    /**
     * 2.2 在当前 Module 合并 Manifest 之后，
     * 使用递归在 /build/output/manifests 文件夹下找寻目标 Manifest.xml 文件
     * 并进行修改
     */
    private void findManifest(File file) {
//        System.out.println("File path : ${file.getAbsolutePath()}   isDir=${file.isDirectory()}")
        // 文件夹，递归遍历
        if (file.isDirectory()) {
            file.listFiles().iterator().each { itemFile ->
                findManifest(itemFile)
            }
        }
        // manifest 文件，修改
        else if (file.exists() && "AndroidManifest.xml".equals(file.getName())) {
            System.out.println("Edit File  --  ${file.getAbsolutePath()} Start.")
            ManifestParser manifestParser = new ManifestParser(file)
            editManifest(manifestParser)
            System.out.println("Edit File  --  ${file.getAbsolutePath()}  OK.")
        }
    }

    /**
     * 3. 找到需要修改的 Manifest 文件，根据要求进行修改
     * @param sourceManifestFile manifest 源文件对象
     */
    private void editManifest(ManifestParser manifestParser) {
        // 3. 遍历修改任务，进行逐次修改
        editExtension.editNodes.each { strArray ->
            // 格式校验,内容格式参考 {@link ManifestEditExtension#editNodes}
            if (strArray == null || strArray.size() < 2) {
                System.out.println("Edit Info Error:  ${strArray}")
                throw new RuntimeException("Edit Info Error:  ${strArray}\n like this：\n" + ManifestEditExtension.EditInfoHint)
            }
            manifestParser.editNode(strArray.get(0), strArray.get(1))
        }
        manifestParser.save()
    }
}