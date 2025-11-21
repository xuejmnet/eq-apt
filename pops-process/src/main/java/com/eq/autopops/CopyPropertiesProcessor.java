package com.eq.autopops;

import com.eq.autopops.process.AutoProperty;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * create time 2024/10/30 09:15
 * 文件说明
 *
 * @author xuejiaming
 */
public class CopyPropertiesProcessor {
    private final JavacTrees javacTrees;
    private final TreeMaker treeMaker;
    private final Names names;
    private final Elements elementUtils;
    private final Trees trees;

    public CopyPropertiesProcessor(JavacTrees javacTrees, TreeMaker treeMaker, Names names, Elements elementUtils, Trees trees) {
        this.javacTrees = javacTrees;
        this.treeMaker = treeMaker;
        this.names = names;
        this.elementUtils = elementUtils;
        this.trees = trees;
    }

    private void collectAnnotations(Set<String> collections,String[] values){

        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if(MyStrUtil.isNotBlank(value)){
                if(value.contains(",")){
                    collections.addAll(MyStrUtil.splitAndRemoveEmptyElements(value,","));
                }else{
                    collections.add(value);
                }
            }
        }
    }

    public void process(EntityTypeMetadata entityTypeMetadata, Element element,Set<String> classProps) {
        AutoProperty annotation = element.getAnnotation(AutoProperty.class);

//            Class<?> source = annotation.source();
        Set<String> properties = new HashSet<>();
        String[] includes = annotation.includes();
        collectAnnotations(properties,includes);
        Set<String> ignores = new HashSet<>(classProps);
        String[] excludes = annotation.excludes();
        collectAnnotations(ignores,excludes);


        addImportInfo(element, entityTypeMetadata.getImportTypes());
        JCTree tree = javacTrees.getTree(element);
        treeMaker.pos = tree.pos;
        tree.accept(new TreeTranslator() {

            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {

                for (EntityTypeProp entityTypeProp : entityTypeMetadata.getTypeProps()) {
                    boolean acceptProp = acceptProp(properties, ignores, entityTypeProp);
                    if(acceptProp){
                        jcClassDecl.defs = jcClassDecl.defs.append(entityTypeProp.jcVariableDecl);
                        jcClassDecl.defs = jcClassDecl.defs.append(makeGetterMethod(entityTypeProp.jcVariableDecl));
                        jcClassDecl.defs = jcClassDecl.defs.append(makeSetterMethod(entityTypeProp.jcVariableDecl));
                    }
                }
            }
        });
    }

    private boolean acceptProp(Set<String> properties, Set<String> ignores, EntityTypeProp entityTypeProp) {
        if (properties != null && !properties.isEmpty()) {
            if(!properties.contains(entityTypeProp.name)){
                return false;
            }
            return ignoreProp(ignores,entityTypeProp);
        }
        return ignoreProp(ignores,entityTypeProp);
    }
    private boolean ignoreProp(Set<String> ignores, EntityTypeProp entityTypeProp) {
        if (ignores != null && !ignores.isEmpty()) {
            if(ignores.contains(entityTypeProp.name)){
                return false;
            }
            return true;
        }
        return true;
    }

    private void addImportInfo(Element element, Set<String> importTypes) {
        TreePath treePath = trees.getPath(element);
        Tree leaf = treePath.getLeaf();
        if (treePath.getCompilationUnit() instanceof JCTree.JCCompilationUnit && leaf instanceof JCTree) {
            JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();

//            for (JCTree jcTree : jccu.getImports()) {
//                if (jcTree != null && jcTree instanceof JCTree.JCImport) {
//                    JCTree.JCImport jcImport = (JCTree.JCImport) jcTree;
//                    if (jcImport.qualid != null && jcImport.qualid instanceof JCTree.JCFieldAccess) {
//                        JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) jcImport.qualid;
//                        try {
//                            if (TRACKER_PACKAGE.equals(jcFieldAccess.selected.toString()) && TRACKER_CLASS.equals(jcFieldAccess.name.toString())) {
//                                return;
//                            }
//                        } catch (NullPointerException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
            java.util.List<JCTree> trees = new ArrayList<>();
            trees.addAll(jccu.defs);
            for (String importType : importTypes) {
                if (importType.contains(".")) {
                    String importPackage = importType.substring(0, importType.lastIndexOf("."));
                    String importClass = importType.substring(importType.lastIndexOf(".") + 1);
                    JCTree.JCIdent ident = treeMaker.Ident(names.fromString(importPackage));
                    JCTree.JCImport jcImport = treeMaker.Import(treeMaker.Select(
                            ident, names.fromString(importClass)), false);
                    if (!trees.contains(jcImport)) {
                        trees.add(0, jcImport);
                    }
                }
            }
            jccu.defs = List.from(trees);
        }
    }


    private Name getterMethodName(JCTree.JCVariableDecl jcVariableDecl) {
        String varName = jcVariableDecl.name.toString();
        Name name = names.fromString("get" + varName.substring(0, 1).toUpperCase() + varName.substring(1, varName.length()));
        return name;
    }

    private Name setterMethodName(JCTree.JCVariableDecl jcVariableDecl) {
        String varName = jcVariableDecl.name.toString();
        Name name = names.fromString("set" + varName.substring(0, 1).toUpperCase() + varName.substring(1, varName.length()));
        return name;
    }

    private JCTree.JCMethodDecl makeGetterMethod(JCTree.JCVariableDecl jcVariableDecl) {
        JCTree.JCModifiers jcModifiers = treeMaker.Modifiers(Flags.PUBLIC);//public
        JCTree.JCExpression retrunType = jcVariableDecl.vartype;//xxx
        Name name = getterMethodName(jcVariableDecl);// getXxx
        JCTree.JCStatement jcStatement = // retrun this.xxx
                treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.name));
        List<JCTree.JCStatement> jcStatementList = List.nil();
        jcStatementList = jcStatementList.append(jcStatement);
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatementList);//构建代码块
        List<JCTree.JCTypeParameter> methodGenericParams = List.nil();//泛型参数列表
        List<JCTree.JCVariableDecl> parameters = List.nil();//参数列表
        List<JCTree.JCExpression> throwsClauses = List.nil();//异常抛出列表
        JCTree.JCExpression defaultValue = null;
        JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(jcModifiers, name, retrunType, methodGenericParams, parameters, throwsClauses, jcBlock, defaultValue);
        return jcMethodDecl;
    }

    private JCTree.JCMethodDecl makeSetterMethod(JCTree.JCVariableDecl jcVariableDecl) {
        JCTree.JCModifiers jcModifiers = treeMaker.Modifiers(Flags.PUBLIC);//public
        JCTree.JCExpression retrunType = treeMaker.TypeIdent(TypeTag.VOID);//或 treeMaker.Type(new Type.JCVoidType())
        Name name = setterMethodName(jcVariableDecl);// setXxx()
        List<JCTree.JCVariableDecl> parameters = List.nil();//参数列表
        JCTree.JCVariableDecl param = treeMaker.VarDef(
                treeMaker.Modifiers(Flags.PARAMETER), jcVariableDecl.name, jcVariableDecl.vartype, null);
//        param.pos = jcVariableDecl.pos;//设置形参这一句不能少，不然会编译报错(java.lang.AssertionError: Value of x -1)
        parameters = parameters.append(param);//添加参数；例如 int age
        JCTree.JCStatement jcStatement = treeMaker.Exec(treeMaker.Assign(
                treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.name),
                treeMaker.Ident(jcVariableDecl.name)));
        List<JCTree.JCStatement> jcStatementList = List.nil();
        jcStatementList = jcStatementList.append(jcStatement);
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatementList);
        List<JCTree.JCTypeParameter> methodGenericParams = List.nil();//泛型参数列表
        List<JCTree.JCExpression> throwsClauses = List.nil();//异常抛出列表
        JCTree.JCExpression defaultValue = null;
        JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(jcModifiers, name, retrunType, methodGenericParams, parameters, throwsClauses, jcBlock, defaultValue);
        return jcMethodDecl;
    }

}
