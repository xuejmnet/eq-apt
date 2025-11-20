package com.eq.autopops;

import com.easy.query.core.annotation.ColumnIgnore;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.eq.autopops.process.CopyIgnore;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * create time 2024/10/30 10:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EntityMetadataContext {

    private static final Map<String, String> TYPE_COLUMN_MAPPING = new HashMap<>();

    static {
        TYPE_COLUMN_MAPPING.put("java.lang.Float", null);
        TYPE_COLUMN_MAPPING.put("java.lang.Double", null);
        TYPE_COLUMN_MAPPING.put("java.lang.Short", null);
        TYPE_COLUMN_MAPPING.put("java.lang.Integer", null);
        TYPE_COLUMN_MAPPING.put("java.lang.Long", null);
        TYPE_COLUMN_MAPPING.put("java.lang.Byte", null);
        TYPE_COLUMN_MAPPING.put("java.math.BigDecimal", "java.math.BigDecimal");
        TYPE_COLUMN_MAPPING.put("java.lang.Boolean", null);
        TYPE_COLUMN_MAPPING.put("java.lang.String", null);
        TYPE_COLUMN_MAPPING.put("java.util.UUID", "java.util.UUID");
        TYPE_COLUMN_MAPPING.put("java.sql.Timestamp", "java.sql.Timestamp");
        TYPE_COLUMN_MAPPING.put("java.sql.Time", "java.sql.Time");
        TYPE_COLUMN_MAPPING.put("java.sql.Date", "java.sql.Date");
        TYPE_COLUMN_MAPPING.put("java.util.Date", "java.util.Date");
        TYPE_COLUMN_MAPPING.put("java.time.LocalDate", "java.time.LocalDate");
        TYPE_COLUMN_MAPPING.put("java.time.LocalDateTime", "java.time.LocalDateTime");
        TYPE_COLUMN_MAPPING.put("java.time.LocalTime", "java.time.LocalTime");
    }

    private final Map<String, EntityTypeMetadata> metadataMap = new ConcurrentHashMap<>();
    private final TreeMaker treeMaker;
    private final Names names;
    private final JavacTrees javacTrees;
    private final Elements elementUtils;
    private final Types typeUtils;

    public EntityMetadataContext(TreeMaker treeMaker, Names names, JavacTrees javacTrees, Elements elementUtils, Types typeUtils) {
        this.treeMaker = treeMaker;
        this.names = names;
        this.javacTrees = javacTrees;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    public EntityTypeMetadata accept(String fullName) {
        return MyUtil.computeIfAbsent(metadataMap, fullName, k -> {

            TypeElement classElement = elementUtils.getTypeElement(fullName);
            HashSet<String> ignoreProperties = new HashSet<>();
            Table tableAnnotation = classElement.getAnnotation(Table.class);
            if (tableAnnotation != null) {
                ignoreProperties.addAll(Arrays.asList(tableAnnotation.ignoreProperties()));
            }
            EntityTypeMetadata entityTypeMetadata = new EntityTypeMetadata(fullName);
            do {
                parseProperty(entityTypeMetadata, classElement, ignoreProperties);
                classElement = (TypeElement) typeUtils.asElement(classElement.getSuperclass());
            } while (classElement != null);
            return entityTypeMetadata;
        });

    }

    private void parseProperty(EntityTypeMetadata entityTypeMetadata, TypeElement classElement, Set<String> ignoreProperties) {
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        for (Element fieldElement : enclosedElements) {

            //all fields
            if (ElementKind.FIELD == fieldElement.getKind()) {

                Set<Modifier> modifiers = fieldElement.getModifiers();
                if (modifiers.contains(Modifier.STATIC)) {
                    //ignore static fields
                    continue;
                }
                String propertyName = fieldElement.toString();
                if (!ignoreProperties.isEmpty() && ignoreProperties.contains(propertyName)) {
                    continue;
                }
                ColumnIgnore column = fieldElement.getAnnotation(ColumnIgnore.class);
                if (column != null) {
                    continue;
                }
                Navigate navigate = fieldElement.getAnnotation(Navigate.class);
                if (navigate != null) {
                    continue;
                }
                CopyIgnore copyIgnore = fieldElement.getAnnotation(CopyIgnore.class);
                if (copyIgnore != null) {
                    continue;
                }


//
//                com.sun.tools.javac.util.List<JCTree.JCAnnotation> annotations = treeMaker.Annotations(compounds);


                List<? extends AnnotationMirror> annotationMirrors = fieldElement.getAnnotationMirrors();
                ArrayList<Attribute.Compound> propAnnotations = new ArrayList<>(annotationMirrors.size());
                for (AnnotationMirror annotationMirror : annotationMirrors) {
                    if (annotationMirror instanceof Attribute.Compound) {
                        Attribute.Compound annotationMirror1 = (Attribute.Compound) annotationMirror;
                        appendTypeImport(entityTypeMetadata, annotationMirror1.type.toString());
                        propAnnotations.add(annotationMirror1);
                    }
                }
                com.sun.tools.javac.util.List<JCTree.JCAnnotation> annotations = treeMaker.Annotations(com.sun.tools.javac.util.List.from(propAnnotations));
                JCTree.JCExpression type = treeMaker.Type(((Symbol.VarSymbol) fieldElement).type);
                JCTree.JCVariableDecl jcVariableDecl = treeMaker.VarDef(treeMaker.Modifiers(Flags.PRIVATE, annotations), names.fromString(propertyName), type, null);
                appendProp(entityTypeMetadata, jcVariableDecl);
//                JCTree tree = javacTrees.getTree(fieldElement);
//                if (tree instanceof JCTree.JCVariableDecl) {
//                    JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
//                    appendProp(entityTypeMetadata,jcVariableDecl);
//                } else {
//
//                    com.sun.tools.javac.util.List<JCTree.JCAnnotation> annotations = treeMaker.Annotations(com.sun.tools.javac.util.List.from(propAnnotations));
//                    JCTree.JCVariableDecl jcVariableDecl = treeMaker.VarDef(treeMaker.Modifiers(Flags.PRIVATE,annotations), names.fromString(propertyName), MyUtil.memberAccess(treeMaker, names, fieldElement.asType().toString()), null);
//                    System.out.println(jcVariableDecl);
//                }
            }
        }
    }

    private void appendProp(EntityTypeMetadata entityTypeMetadata, JCTree.JCVariableDecl jcVariableDecl) {
        EntityTypeProp entityTypeProp = new EntityTypeProp(jcVariableDecl);
        entityTypeMetadata.getTypeProps().add(entityTypeProp);
        JCTree.JCExpression vartype = jcVariableDecl.vartype;
        Type type = vartype.type;
        appendTypeImport(entityTypeMetadata, type.toString());
    }

    private void appendTypeImport(EntityTypeMetadata entityTypeMetadata, String fullTypeName) {
        if (TYPE_COLUMN_MAPPING.containsKey(fullTypeName)) {
            String needImport = TYPE_COLUMN_MAPPING.get(fullTypeName);
            if (needImport != null) {
                entityTypeMetadata.getImportTypes().add(needImport);
            }
        } else {
            List<String> strings = MyUtil.parseGenericTypes(fullTypeName);
            entityTypeMetadata.getImportTypes().addAll(strings);
        }
    }
}
