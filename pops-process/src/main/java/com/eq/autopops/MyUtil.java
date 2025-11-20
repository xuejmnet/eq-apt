package com.eq.autopops;

import com.eq.autopops.process.AutoProperty;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * create time 2024/10/30 10:41
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyUtil {
    //传入一个类的全路径名，获取对应类的JCIdent
    public static JCTree.JCExpression memberAccess(TreeMaker treeMaker, Names names, String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(componentArray[i]));
        }
        return expr;
    }
    public static String getClassFromAnnotation(Element key, String valueName) {
        List<? extends AnnotationMirror> annotationMirrors = key.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (AutoProperty.class.getName().equals(annotationMirror.getAnnotationType().toString())) {
                Set<? extends ExecutableElement> keySet = annotationMirror.getElementValues().keySet();
                for (ExecutableElement executableElement : keySet) {
                    if (Objects.equals(executableElement.getSimpleName().toString(), valueName)) {
                        return annotationMirror.getElementValues().get(executableElement).getValue().toString();
                    }
                }
            }
        }
        return null;
    }
    public static <K,V> V computeIfAbsent(Map<K,V> map, K key, Function<? super K, ? extends V> mappingFunction){
        V v=null;
        if(null==(v=map.get(key))){
            v=map.computeIfAbsent(key,mappingFunction);
        }
        return v;
    }
    public static List<String> parseGenericTypes(String typeString) {
        if(!typeString.contains("<")){
            return Collections.singletonList(typeString);
        }
        List<String> types = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int angleBrackets = 0;

        for (char c : typeString.toCharArray()) {
            if (c == '<') {
                if (angleBrackets == 0) {
                    // 处理主类型
                    String mainType = sb.toString().trim();
                    if (!mainType.isEmpty()) {
                        types.add(mainType);
                    }
                    sb.setLength(0); // 清空 StringBuilder
                }
                angleBrackets++;
            } else if (c == '>') {
                angleBrackets--;
                if (angleBrackets == 0) {
                    types.add(sb.toString().trim());
                    sb.setLength(0); // 清空 StringBuilder
                } else {
                    sb.append(c);
                }
            } else if (angleBrackets > 0 || c == ',') {
                sb.append(c);
            } else {
                sb.append(c);
            }
        }

        // 处理最后一个类型（如果存在）
        if (sb.length() > 0) {
            types.add(sb.toString().trim());
        }

        return types;
    }
}
