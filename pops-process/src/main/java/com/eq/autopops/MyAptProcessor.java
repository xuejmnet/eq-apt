package com.eq.autopops;

import com.eq.autopops.process.AutoProperty;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * create time 2025/11/20 21:42
 * 文件说明
 *
 * @author xuejiaming
 */
@SupportedAnnotationTypes({"com.eq.autopops.process.AutoProperty"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MyAptProcessor extends AbstractProcessor {

    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;
    private Elements elementUtils;
    private Types typeUtils;
    private Trees trees;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        JavacProcessingEnvironment javacProcessingEnvironment = getJavacProcessingEnvironment(processingEnv);
        Context context = javacProcessingEnvironment.getContext();
        this.javacTrees = JavacTrees.instance(context);
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.trees = Trees.instance(javacProcessingEnvironment);
    }

    /**
     * This class casts the given processing environment to a JavacProcessingEnvironment. In case of
     * gradle incremental compilation, the delegate ProcessingEnvironment of the gradle wrapper is returned.
     */
    public JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv) {
        if (procEnv instanceof JavacProcessingEnvironment) return (JavacProcessingEnvironment) procEnv;

        // try to find a "delegate" field in the object, and use this to try to obtain a JavacProcessingEnvironment
        for (Class<?> procEnvClass = procEnv.getClass(); procEnvClass != null; procEnvClass = procEnvClass.getSuperclass()) {
            Object delegate = tryGetDelegateField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProxyDelegateToField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProcessingEnvField(procEnvClass, procEnv);

            if (delegate != null) return getJavacProcessingEnvironment(delegate);
            // delegate field was not found, try on superclass
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Can't get the delegate of the gradle IncrementalProcessingEnvironment. Lombok won't work.");
        return null;
    }

    /**
     * Gradle incremental processing
     */
    private Object tryGetDelegateField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "delegate").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kotlin incremental processing
     */
    private Object tryGetProcessingEnvField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "processingEnv").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IntelliJ IDEA >= 2020.3
     */
    private Object tryGetProxyDelegateToField(Class<?> delegateClass, Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> sets = roundEnv.getElementsAnnotatedWith(AutoProperty.class);
        EntityMetadataContext entityMetadataContext = new EntityMetadataContext(treeMaker, names, javacTrees, elementUtils, typeUtils);
//        AutoPropGroupContext autoPropGroupContext = new AutoPropGroupContext(elementUtils);
        CopyPropertiesProcessor copyPropertiesProcessor = new CopyPropertiesProcessor(javacTrees, treeMaker, names, elementUtils, trees);
        for (Element element : sets) {
// 生成参数 例如：private String age;
//            treeMaker.VarDef(treeMaker.Modifiers(Flags.PRIVATE), names.fromString("age"), treeMaker.Ident(names.fromString("String")), null);
            String classFromAnnotation = MyUtil.getClassFromAnnotation(element, "value");
            EntityTypeMetadata entityTypeMetadata = entityMetadataContext.accept(classFromAnnotation);
            TypeElement classElement = (TypeElement) element;
            Set<String> classProps = getClassProps(classElement);
            copyPropertiesProcessor.process(entityTypeMetadata, element, classProps);

        }
        return true;
    }

    /**
     * 解析当前class已经存在的字段,那么如果存在就不要拷贝了
     * @param classElement
     * @return
     */
    private Set<String> getClassProps(TypeElement classElement) {
        Set<String> classProps = new HashSet<>();
        java.util.List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        for (Element fieldElement : enclosedElements) {

            //all fields
            if (ElementKind.FIELD == fieldElement.getKind()) {


                Set<Modifier> modifiers = fieldElement.getModifiers();
                if (modifiers.contains(Modifier.STATIC)) {
                    //ignore static fields
                    continue;
                }

                String propertyName = fieldElement.toString();
                classProps.add(propertyName);
            }
        }
        return classProps;
    }
}
