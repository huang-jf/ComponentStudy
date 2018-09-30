package com.hjf.router.compiler;

import com.google.auto.service.AutoService;
import com.hjf.router.facade.annotation.Autowired;
import com.hjf.router.facade.annotation.Route;
import com.hjf.router.utils.RouterJavaFilePathUtil;
import com.hjf.router.compiler.util.Constants;
import com.hjf.router.compiler.util.FileUtils;
import com.hjf.router.compiler.util.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

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
@SupportedOptions(Constants.KEY_HOST_NAME)
public class RouterProcessor extends AbstractProcessor {

    private static final String Route_Mapper_Field_Name = "serviceMapper";

    private Logger logger;

    private Filer mFiler;
    private Types types;
    private Elements elements;

    private TypeMirror type_String;

    private ArrayList<RouteNode> routeNodes;
    private String host = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        routeNodes = new ArrayList<>();

        mFiler = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();

        type_String = elements.getTypeElement("java.lang.String").asType();

        logger = new Logger(processingEnv.getMessager());

        Map<String, String> options = processingEnv.getOptions();
        if (options != null && !options.isEmpty()) {
            host = options.get(Constants.KEY_HOST_NAME);
            logger.info(">>> host is " + host + " <<<");
        }
        if (host == null || host.equals("")) {
            host = "default";
        }
        logger.info(">>> RouteProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set != null && !set.isEmpty()) {
            Set<? extends Element> routeNodes = roundEnvironment.getElementsAnnotatedWith(Route.class);
            try {
                logger.info(">>> Found routes, start... <<<");
                parseRouteNodes(routeNodes);
            } catch (Exception e) {
                logger.error(e);
            }
            generateRouterImpl();
            generateRouterTable();
            return true;
        }
        return false;
    }

    /**
     * generate HostRouterTable.txt
     */
    private void generateRouterTable() {
        String fileName = RouterJavaFilePathUtil.genRouterTable(host);
        if (FileUtils.createFile(fileName)) {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("auto generated, do not change !!!! \n\n")
                    .append("HOST : ")
                    .append(host).append("\n\n");

            for (RouteNode routeNode : routeNodes) {
                stringBuilder.append(routeNode.getDesc()).append("\n");
                stringBuilder.append(routeNode.getPath()).append("\n");
                Map<String, String> paramsType = routeNode.getParamsTypeDesc();
                if (paramsType != null && !paramsType.isEmpty()) {
                    for (Map.Entry<String, String> types : paramsType.entrySet()) {
                        stringBuilder.append(types.getKey())
                                .append(":")
                                .append(types.getValue())
                                .append("\n");
                    }
                }
                stringBuilder.append("\n");
            }
            FileUtils.writeStringToFile(fileName, stringBuilder.toString(), false);
        }
    }


    /**
     * generate HostUIRouter.java
     */
    private void generateRouterImpl() {

        String claName = RouterJavaFilePathUtil.genModuleClientClassPath(host);

        //pkg
        String pkg = claName.substring(0, claName.lastIndexOf("."));
        //simpleName
        String cn = claName.substring(claName.lastIndexOf(".") + 1);
        // superClassName

        TypeElement typeBaseCompRouter = elements.getTypeElement(Constants.BASE_DNS_SERVICE);

        if (typeBaseCompRouter == null) {
            logger.error(Constants.MISSING_BASE_COMP_ROUTER_MSG);
            return;
        }

        ClassName superClass = ClassName.get(typeBaseCompRouter);

        MethodSpec initHostMethod = generateInitHostMethod();
        MethodSpec initMapMethod = generateInitMapMethod();

        try {
            JavaFile.builder(pkg, TypeSpec.classBuilder(cn)
                    .addModifiers(PUBLIC)
                    .superclass(superClass)
                    .addMethod(initHostMethod)
                    .addMethod(initMapMethod)
                    .build()
            ).build().writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseRouteNodes(Set<? extends Element> routeElements) {

        TypeMirror type_Activity = elements.getTypeElement(Constants.ACTIVITY).asType();

        for (Element element : routeElements) {
            TypeMirror tm = element.asType();
            Route route = element.getAnnotation(Route.class);

            if (types.isSubtype(tm, type_Activity)) {                 // Activity
                logger.info(">>> Found activity route: " + tm.toString() + " <<<");

                RouteNode routeNode = new RouteNode();
                String path = route.path();

                checkPath(path);

                routeNode.setPath(path);
                routeNode.setDesc(route.desc());
                routeNode.setRawType(element);

                Map<String, String> paramsDesc = new HashMap<>();
                for (Element field : element.getEnclosedElements()) {
                    if (field.getKind().isField() && field.getAnnotation(Autowired.class) != null) {
                        Autowired paramConfig = field.getAnnotation(Autowired.class);
                        paramsDesc.put(paramConfig.name().isEmpty()
                                ? field.getSimpleName().toString() : paramConfig.name(), field.asType().toString());
                    }
                }
                routeNode.setParamsTypeDesc(paramsDesc);

                if (!routeNodes.contains(routeNode)) {
                    routeNodes.add(routeNode);
                }
            } else {
                throw new IllegalStateException("only activity can be annotated by RouteNode");
            }
        }
    }

    private void checkPath(String path) {
        if (path == null || path.isEmpty() || !path.startsWith("/"))
            throw new IllegalArgumentException("path cannot be null or empty,and should start with /,this is:" + path);

        if (path.contains("//") || path.contains("&") || path.contains("?"))
            throw new IllegalArgumentException("path should not contain // ,& or ?,this is:" + path);

        if (path.endsWith("/"))
            throw new IllegalArgumentException("path should not endWith /,this is:" + path
                    + ";or append a token:index");
    }

    /**
     * create init host method
     */
    private MethodSpec generateInitHostMethod() {
        TypeName returnType = TypeName.get(type_String);

        MethodSpec.Builder openUriMethodSpecBuilder = MethodSpec.methodBuilder("getClientHost")
                .returns(returnType)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        openUriMethodSpecBuilder.addStatement("return $S", host);

        return openUriMethodSpecBuilder.build();
    }

    /**
     * create init map method
     */
    private MethodSpec generateInitMapMethod() {
        TypeName returnType = TypeName.VOID;

        MethodSpec.Builder openUriMethodSpecBuilder = MethodSpec.methodBuilder("inputService")
                .returns(returnType)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        openUriMethodSpecBuilder.addStatement("super.inputService()");

        for (RouteNode routeNode : routeNodes) {
            openUriMethodSpecBuilder.addStatement(
                    Route_Mapper_Field_Name + ".put($S,$T.class)",
                    routeNode.getPath(), ClassName.get((TypeElement) routeNode.getRawType()));
        }

        return openUriMethodSpecBuilder.build();
    }
}
