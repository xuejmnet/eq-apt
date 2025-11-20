package com.eq.autopops;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2024/10/30 10:35
 * 文件说明
 *
 * @author xuejiaming
 */
public class EntityTypeMetadata {
    private final String fullName;
    private final List<EntityTypeProp> typeProps;
    private final Set<String> importTypes;

    public EntityTypeMetadata(String fullName){
        this.fullName = fullName;
        this.typeProps = new ArrayList<>();
        this.importTypes = new LinkedHashSet<>();
    }

    public String getFullName() {
        return fullName;
    }

    public List<EntityTypeProp> getTypeProps() {
        return typeProps;
    }

    public Set<String> getImportTypes() {
        return importTypes;
    }
}
