package com.github.cheukbinli.original.sql.parser.model;

import java.io.Serializable;

public class SQLMetaInfo implements Serializable {

    private static final long serialVersionUID = -7008133848902957291L;
    private String tableName;
    private String aliasName;
//    private Object parent;

    public SQLMetaInfo() {
    }

    public SQLMetaInfo(String tableName, String aliasName) {
        this.tableName = tableName;
        this.aliasName = aliasName;
    }
//    public SQLMetaInfo(String tableName, String aliasName, Object parent) {
//        this.tableName = tableName;
//        this.aliasName = aliasName;
//        this.parent = parent;
//    }

    public String getTableName() {
        return tableName;
    }

    public SQLMetaInfo setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public String getAliasName() {
        return aliasName;
    }

    public SQLMetaInfo setAliasName(String aliasName) {
        this.aliasName = aliasName;
        return this;
    }

//    public <T> T getParent() {
//        return (T) parent;
//    }
//
//    public SQLMetaInfo setParent(Object parent) {
//        this.parent = parent;
//        return this;
//    }
}
