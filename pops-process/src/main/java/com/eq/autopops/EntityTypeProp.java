package com.eq.autopops;

import com.sun.tools.javac.tree.JCTree;

/**
 * create time 2024/10/30 14:02
 * 文件说明
 *
 * @author xuejiaming
 */
public class EntityTypeProp {

    public final JCTree.JCVariableDecl jcVariableDecl;
    public final String name;

    public EntityTypeProp(JCTree.JCVariableDecl jcVariableDecl){
        this.jcVariableDecl = jcVariableDecl;
        this.name = jcVariableDecl.name.toString();
    }
}
