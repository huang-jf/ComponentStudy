package com.hjf.router.compiler;

import com.google.auto.service.AutoService;
import com.hjf.router.facade.annotation.Autowired;
import com.hjf.router.utils.RouterJavaFilePathUtil;
import com.hjf.router.compiler.util.Constants;
import com.hjf.router.compiler.util.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "com.hjf.routerlib.annotation.Autowired",
})
public class AutowiredProcessor extends AbstractProcessor {
    private static final String TAG = AutowiredProcessor.class.getSimpleName();

    private Logger logger;

    private Filer mFiler;
    private Types types;
    private Elements elements;

    /**
     * Contain field need autowired and his super class.
     */
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();

    private static final ClassName AndroidLog = ClassName.get("android.util", "Log");

    private static final ClassName NullPointerException = ClassName.get("java.lang", "NullPointerException");

    private static final ClassName ParamException = ClassName.get("com.hjf.routerlib.exceptions", "ParamException");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        mFiler = processingEnv.getFiler();                  // Generate class.
        types = processingEnv.getTypeUtils();            // Get type utils.
        elements = processingEnv.getElementUtils();      // Get class meta.

        logger = new Logger("AutowiredProcessor", processingEnv.getMessager());   // Package the log utils.

        logger.info(">>> AutowiredProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set != null && !set.isEmpty()) {
            try {
                logger.info(">>> Found autowired field, start... <<<");
                categories(roundEnvironment.getElementsAnnotatedWith(Autowired.class));
                generateHelper();
            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void generateHelper() throws IOException, IllegalAccessException {
        TypeElement type_ISyringe = elements.getTypeElement(Constants.ROUTE_SYRINGE);


        TypeMirror activityTm = elements.getTypeElement(Constants.ACTIVITY).asType();
        TypeMirror fragmentTm = elements.getTypeElement(Constants.FRAGMENT).asType();
        TypeMirror fragmentTmV4 = elements.getTypeElement(Constants.FRAGMENT_V4).asType();
        TypeMirror bundleTm = elements.getTypeElement(Constants.BUNDLE).asType();


        // Build input param name.
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();

        ParameterSpec bundleParamSpec = ParameterSpec
                .builder(ParameterizedTypeName.get(bundleTm), "bundle")
                .build();

        if (parentAndChild != null && !parentAndChild.isEmpty()) {
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                // Build method : 'inject'
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("inject")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(objectParamSpec);

                //Build method: void preCondition(Bundle bundle);
                MethodSpec.Builder preConditionMethodBuilder = MethodSpec.methodBuilder("preCondition")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addException(ParamException)
                        .addParameter(bundleParamSpec);

                TypeElement parent = entry.getKey();
                List<Element> children = entry.getValue();

                String qualifiedName = parent.getQualifiedName().toString();
                String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));

                String fileName = RouterJavaFilePathUtil.getAutowiredJavaFilePath(parent.getSimpleName().toString());

                logger.info(">>> Start process " + children.size() + " field in " + parent.getSimpleName() + " ... <<<");

                TypeSpec.Builder helper = TypeSpec.classBuilder(fileName)
                        .addJavadoc("Auto generated by " + TAG)
                        .addSuperinterface(ClassName.get(type_ISyringe))
                        .addModifiers(PUBLIC);

                injectMethodBuilder.addStatement("$T substitute = ($T)target", ClassName.get(parent), ClassName.get(parent));

                // Generate method body, start inject.
                for (Element element : children) {
                    Autowired autowired = element.getAnnotation(Autowired.class);
                    String fieldName = element.getSimpleName().toString();
                    String defaultValue = "substitute." + fieldName;
                    logger.info(">>> autowired field name[" + fieldName + "] type[" + element.asType().toString() + "]  <<<");

                    StringBuilder statementBuilder = new StringBuilder();
                    statementBuilder.append("substitute.").append(fieldName).append(" = ");
                    statementBuilder.append("substitute.");
                    // activity
                    if (types.isSubtype(parent.asType(), activityTm)) {  // Activity, then use getIntent()
                        statementBuilder.append("getIntent().");
                        statementBuilder.append(buildStatement(element.asType().toString(), true, defaultValue));
                    }
                    // fragment
                    else if (types.isSubtype(parent.asType(), fragmentTm) ||
                            types.isSubtype(parent.asType(), fragmentTmV4)) {   // Fragment, then use getArguments()
                        statementBuilder.append("getArguments().");
                        statementBuilder.append(buildStatement(element.asType().toString(), false, defaultValue));
                    }
                    // other
                    else {
                        throw new IllegalAccessException("The field [" + fieldName + "] need " +
                                "autowired from intent, its parent must be activity or fragment!");
                    }

                    injectMethodBuilder.addStatement(statementBuilder.toString(), autowired.name().length() == 0 ? fieldName : autowired.name());

                    // Validator
                    if (autowired.required() && !element.asType().getKind().isPrimitive()) {  // Primitive wont be check.
                        injectMethodBuilder.beginControlFlow("if (null == substitute." + fieldName + ")");
                        injectMethodBuilder.addStatement(
                                "$T.e(\"" + TAG + "\", \"The field '" + fieldName + "' is null," + "field description is:" + autowired.desc() +
                                        ",in class '\" + $T.class.getName() + \"!\")", AndroidLog, ClassName.get(parent));

                        injectMethodBuilder.endControlFlow();
                    }

                    //preCondition
                    if (autowired.required()) {
                        preConditionMethodBuilder.beginControlFlow("if (!bundle.containsKey(\"" + fieldName + "\"))");

                        preConditionMethodBuilder.addStatement("throw new $T(" +
                                "\"" + fieldName + "\")", ParamException);

                        preConditionMethodBuilder.endControlFlow();
                    }
                }

                helper.addMethod(injectMethodBuilder.build());


                ////////////////
                helper.addMethod(preConditionMethodBuilder.build());


                // Generate autowire helper
                JavaFile.builder(packageName, helper.build()).build().writeTo(mFiler);

                logger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }

            logger.info(">>> Autowired processor stop. <<<");
        }
    }

    /**
     * @param paramTypeDef type of data in the  bundle
     */
    private String buildStatement(String paramTypeDef, boolean isActivity, String defaultValue) {

        // boolean
        if ("boolean".equals(paramTypeDef) || "java.lang.Boolean".equals(paramTypeDef)) {
            return (isActivity ? ("getBooleanExtra($S, " + defaultValue + ")") : ("getBoolean($S)"));
        }
        // byte
        else if ("byte".equals(paramTypeDef) || "java.lang.Byte".equals(paramTypeDef)) {
            return (isActivity ? ("getByteExtra($S, " + defaultValue + ")") : ("getByte($S)"));
        }
        // short
        else if ("short".equals(paramTypeDef) || "java.lang.Short".equals(paramTypeDef)) {
            return (isActivity ? ("getShortExtra($S, " + defaultValue + ")") : ("getShort($S)"));
        }
        // int
        else if ("int".equals(paramTypeDef) || "java.lang.Integer".equals(paramTypeDef)) {
            return (isActivity ? ("getIntExtra($S, " + defaultValue + ")") : ("getInt($S)"));
        }
        // long
        else if ("long".equals(paramTypeDef) || "java.lang.Long".equals(paramTypeDef)) {
            return (isActivity ? ("getLongExtra($S, " + defaultValue + ")") : ("getLong($S)"));
        }
        // float
        else if ("float".equals(paramTypeDef) || "java.lang.Float".equals(paramTypeDef)) {
            return (isActivity ? ("getFloatExtra($S, " + defaultValue + ")") : ("getFloat($S)"));
        }
        // double
        else if ("double".equals(paramTypeDef) || "java.lang.Double".equals(paramTypeDef)) {
            return (isActivity ? ("getDoubleExtra($S, " + defaultValue + ")") : ("getDouble($S)"));
        }
        // char
        else if ("char".equals(paramTypeDef) || "java.lang.Character".equals(paramTypeDef)) {
            return (isActivity ? ("getCharExtra($S, " + defaultValue + ")") : ("getChar($S)"));
        }
        // string
        else if ("java.lang.String".equals(paramTypeDef)) {
            return (isActivity ? ("getStringExtra($S)") : ("getString($S)"));
        }
        // serializable
        else if ("java.io.Serializable".equals(paramTypeDef)) {
            return (isActivity ? ("getSerializableExtra($S)") : ("getSerializable($S)"));
        }
        // parcelable
        else if ("android.os.Parcelable".equals(paramTypeDef)) {
            return (isActivity ? ("getParcelableExtra($S)") : ("getParcelable($S)"));
        } else {

        }
        // Unknown
        return " unknown param type: " + paramTypeDef + "  ";
    }


    /**
     * Categories field, find his papa.
     *
     * @param elements Field need autowired
     */
    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (elements != null && !elements.isEmpty()) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The autowired fields CAN NOT BE 'private'!!! please check field ["
                            + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }

                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> children = new ArrayList<>();
                    children.add(element);
                    parentAndChild.put(enclosingElement, children);
                }
            }
            logger.info("categories finished.");
        }
    }
}
