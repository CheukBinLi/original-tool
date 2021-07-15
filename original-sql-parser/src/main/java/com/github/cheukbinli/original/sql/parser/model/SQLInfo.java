package com.github.cheukbinli.original.sql.parser.model;

import java.io.Serializable;
import java.util.Map;

public class SQLInfo implements Serializable {

    private static final long serialVersionUID = 3122512942260594875L;
    private String tableName;
    private String columns;
    private String orderBy;
    private String where;
    private String groupBy;
    private String originSQL;
    private String rebuildSQL;
    private String finalSQL;
    private String operationName;
    private String groupName;
    private Map<String, Object> param;

    public SQLInfo() {
    }

    public SQLInfo(String originSQL, String rebuildSQL, String finalSQL, Map<String, Object> param) {
        this.originSQL = originSQL;
        this.finalSQL = finalSQL;
        this.param = param;
        this.rebuildSQL = rebuildSQL;
    }

    public SQLInfo(String originSQL, String rebuildSQL, String finalSQL, String operationName, String groupName, Map<String, Object> param) {
        this.originSQL = originSQL;
        this.finalSQL = finalSQL;
        this.param = param;
        this.operationName = operationName;
        this.groupName = groupName;
        this.rebuildSQL = rebuildSQL;
    }

    public SQLInfo(String tableName, String columns, String where, String groupBy, String orderBy, String originSQL, String rebuildSQL, String finalSQL, Map<String, Object> param) {
        this.tableName = tableName;
        this.columns = columns;
        this.orderBy = orderBy;
        this.where = where;
        this.originSQL = originSQL;
        this.finalSQL = finalSQL;
        this.param = param;
        this.groupBy = groupBy;
        this.rebuildSQL = rebuildSQL;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getOriginSQL() {
        return originSQL;
    }

    public void setOriginSQL(String originSQL) {
        this.originSQL = originSQL;
    }

    public String getFinalSQL() {
        return finalSQL;
    }

    public SQLInfo setFinalSQL(String finalSQL) {
        this.finalSQL = finalSQL;
        return this;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public Map<String, Object> getParam() {
        return param;
    }

    public SQLInfo setParam(Map<String, Object> param) {
        this.param = param;
        return this;
    }

    public String getRebuildSQL() {
        return rebuildSQL;
    }

    public void setRebuildSQL(String rebuildSQL) {
        this.rebuildSQL = rebuildSQL;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
