package com.hjf.router.compiler;

import com.google.auto.service.AutoService;
import com.hjf.router.compiler.util.Constants;
import com.hjf.router.compiler.util.FileUtils;
import com.hjf.router.compiler.util.Logger;
import com.hjf.router.facade.annotation.Route;
import com.hjf.router.utils.RouterJavaFilePathUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


/*
 * $L: 直接使用 android.support.v4.app.ActivityCompat.startActivity()
 * $T: 导入后再使用 import android.support.v4.app.ActivityCompat;  ActivityCompat.startActivity();
 * $S: 字符串，对于丢如内容强制加上引用符号， "content"
 */
/*
 * Element 介绍
 *
 * package com.example; // PackageElement
 *
 * public class Foo { // TypeElement private
 *
 *      int a; // VariableElement
 *
 *      private Foo other; // VariableElement
 *
 *      public Foo () {} // ExecuteableElement
 *
 *      public void setA ( // ExecuteableElement
 *          int newA // TypeElement ) {
 *      }
 * }
 */
/* 自动生成 javax.annotation.processing.IProcessor 文件 */
@AutoService(Processor.class)
/* java版本支持 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
/*
 * 将指定注解 注册 到 此注解处理器 上
 * 若没有注册注解或是注册的注解没被使用，不会生成Java文件
 */
@SupportedAnnotationTypes({
        "com.hjf.router.facade.annotation.Route",
})
@SupportedOptions(Constants.OPTION_MODULE_NAME)
public class RouteGroupProcessor extends AbstractProcessor {

    private Logger logger;
    private Filer mFiler;
    private Types typeUtil;
    private Elements elementUtil;

    private String moduleName = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mFiler = processingEnv.getFiler();
        typeUtil = processingEnv.getTypeUtils();
        elementUtil = processingEnv.getElementUtils();
        logger = new Logger("RouteGroupProcessor", processingEnv.getMessager());

        Map<String, String> options = processingEnv.getOptions();
        if (options != null && !options.isEmpty()) {
            moduleName = options.get(Constants.OPTION_MODULE_NAME);
            logger.info("moduleName is " + moduleName + " ");
        }
        if (moduleName == null || moduleName.equals("")) {
            moduleName = "default";
        }
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set == null || set.isEmpty()) {
            logger.info("set is empty ");
            return false;
        }

        // Override method loadInto
        ParameterizedTypeName mapTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_META)));
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("loadInto")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(mapTypeName, "atlas").build());


        // 1. @Route -> Activity、Fragment、IProvider ...
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        if (routeElements == null) {
            logger.info("found route element annotation num is null");
            return true;
        }

        logger.info("found route element annotation num is : " + routeElements.size());
        for (Element element : routeElements) {
            Route route = element.getAnnotation(Route.class);
            logger.info("current element route annotation: " +
                    " path=" + route.path() +
                    " name=" + route.name());
            // Activity 派生类
            if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.ACTIVITY).asType())) {
                logger.info("current element is Activity ");
                methodBuilder.addStatement("atlas.put($S, $T.build($T.ACTIVITY, $T.class, $S, $S, null, -1, -1))",
                        route.path(),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_META)),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_TYPE)),
                        element,
                        route.path(),
                        route.group());
            }
            // Fragment 派生类
            else if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.FRAGMENT).asType())
                    || typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.FRAGMENT_V4).asType())) {
                methodBuilder.addStatement("atlas.put($S, $T.build($T.FRAGMENT, $T.class, $S, $S, null, -1, -1))",
                        route.path(),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_META)),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_TYPE)),
                        element,
                        route.path(),
                        route.group());
            }
            // IProvider 派生类
            else if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.ROUTE_PROVIDER).asType())) {
                methodBuilder.addStatement("atlas.put($S, $T.build($T.PROVIDER, $T.class, $S, $S, null, -1, -1))",
                        route.path(),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_META)),
                        ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_TYPE)),
                        element,
                        route.path(),
                        route.group());
            }
            // other not support type
            else {
                throw new IllegalStateException("not support type[" + element.asType().toString() + "] " +
                        " to used annotation Route");
            }
        }

        // 3. build class
        String implClassPath = RouterJavaFilePathUtil.getRouterGroupImplClassPath(moduleName);
        String rootPackageName = implClassPath.substring(0, implClassPath.lastIndexOf("."));
        String rootSimpleName = implClassPath.substring(implClassPath.lastIndexOf(".") + 1);
        logger.info("build class: " + implClassPath);
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(rootSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_GROUP)))
                .addMethod(methodBuilder.build());


        // 4. write java file
        logger.info("write java file");
        try {
            JavaFile.builder(rootPackageName, classBuilder.build())
                    .build()
                    .writeTo(mFiler);
        } catch (IOException e) {
            return false;
        }

        // 写出路由表
        generateRouterTable(routeElements);

        // RouteRootProcessor 需要用到此事件，不能返回true
        // 返回 true 之后不再分发此注解事件
        return false;
    }


    /**
     * generate HostRouterTable.txt
     */
    private void generateRouterTable(Set<? extends Element> routeElements) {
        String fileName = RouterJavaFilePathUtil.genRouterTableFilePath(moduleName);
        if (FileUtils.createFile(fileName)) {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("auto generated, do not change !!!! \n\n")
                    .append("Module --> ").append(moduleName).append("\n\n");
            for (Element element : routeElements) {
                Route route = element.getAnnotation(Route.class);
                // class name[ClassType]
                // Activity 派生类
                if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.ACTIVITY).asType())) {
                    stringBuilder.append(element.getSimpleName()).append(" [ACTIVITY]").append("\n");
                }
                // Fragment 派生类
                else if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.FRAGMENT).asType())
                        || typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.FRAGMENT_V4).asType())) {
                    stringBuilder.append(element.getSimpleName()).append(" [Fragment]").append("\n");
                }
                // IProvider 派生类
                else if (typeUtil.isSubtype(element.asType(), elementUtil.getTypeElement(Constants.ROUTE_PROVIDER).asType())) {
                    stringBuilder.append(element.getSimpleName()).append(" [IProvider]").append("\n");
                }
                else {
//                    stringBuilder.append(element.getSimpleName()).append(" [Unknown]").append("\n");
                }
                stringBuilder
                        // path
                        .append("\t").append("path:\t").append(route.path()).append("\n")
                        // name
                        .append("\t").append("name:\t").append(route.name()).append("\n")
                        // todo param
//                        .append("\t").append("param:\t").append(route.name()).append("\n")
                        .append("\n");
            }
            FileUtils.writeStringToFile(fileName, stringBuilder.toString(), false);
        }
    }
}
