package com.hjf.router.compiler;

import com.google.auto.service.AutoService;
import com.hjf.router.compiler.util.Constants;
import com.hjf.router.compiler.util.Logger;
import com.hjf.router.facade.annotation.Route;
import com.hjf.router.utils.RouterJavaFilePathUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.lang.reflect.TypeVariable;
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
public class RouteRootProcessor extends AbstractProcessor {

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
        logger = new Logger("RouteRootProcessor", processingEnv.getMessager());

        Map<String, String> options = processingEnv.getOptions();
        if (options != null && !options.isEmpty()) {
            moduleName = options.get(Constants.OPTION_MODULE_NAME);
            logger.info(" moduleName is " + moduleName + " ");
        }
        if (moduleName == null || moduleName.equals("")) {
            moduleName = "default";
        }
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set == null || set.isEmpty()) {
            logger.info(" set is empty ");
            return false;
        }

        // Override method loadInto
        logger.info(" start build method loadInto ... ");
        ParameterizedTypeName mapTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                TypeVariableName.get("Class<? extends " + Constants.ROUTE_GROUP + ">"));
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("loadInto")
                .addJavadoc("@param routes {@link $T}", ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_GROUP)))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(mapTypeName, "routes").build());


        // 1. @Route -> Activity
        // put route group
        logger.info(" start found route ... ");
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        if (routeElements != null && !routeElements.isEmpty()) {
            logger.info(" route annotation num is: " + routeElements.size());
            String routerGroupImplClassPath = RouterJavaFilePathUtil.getRouterGroupImplClassPath(moduleName);
            String groupPackageName = routerGroupImplClassPath.substring(0, routerGroupImplClassPath.lastIndexOf("."));
            String groupSimpleName = routerGroupImplClassPath.substring(routerGroupImplClassPath.lastIndexOf(".") + 1);
            methodBuilder.addStatement("routes.put($S, $T.class)", moduleName, ClassName.get(groupPackageName, groupSimpleName));
        }
        // 不用添加存入代码
        else {
            logger.info(" not found route annotation for module[" + moduleName + "]");
        }
        // 2. IProvider ...


        // 3. build class
        String routerRootImplClassPath = RouterJavaFilePathUtil.getRouterRootImplClassPath(moduleName);
        String rootPackageName = routerRootImplClassPath.substring(0, routerRootImplClassPath.lastIndexOf("."));
        String rootSimpleName = routerRootImplClassPath.substring(routerRootImplClassPath.lastIndexOf(".") + 1);
        logger.info("build class: " + routerRootImplClassPath);
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(rootSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(elementUtil.getTypeElement(Constants.ROUTE_ROOT)))
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
        // 返回 true 之后不再分发此注解事件
        return false;
    }
}
