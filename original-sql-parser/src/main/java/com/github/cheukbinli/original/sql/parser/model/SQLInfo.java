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
    private String originSql;
    private String sql;
    private String operationName;
    private String groupName;
    private Map<String, Object> param;

    public SQLInfo() {
    }

    public SQLInfo(String originSql, String sql, Map<String, Object> param) {
        this.originSql = originSql;
        this.sql = sql;
        this.param = param;
    }

    public SQLInfo(String originSql, String sql, String operationName, String groupName, Map<String, Object> param) {
        this.originSql = originSql;
        this.sql = sql;
        this.param = param;
        this.operationName = operationName;
        this.groupName = groupName;
    }

    public SQLInfo(String tableName, String columns, String where, String groupBy, String orderBy, String originSql, String sql, Map<String, Object> param) {
        this.tableName = tableName;
        this.columns = columns;
        this.orderBy = orderBy;
        this.where = where;
        this.originSql = originSql;
        this.sql = sql;
        this.param = param;
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

    public String getOriginSql() {
        return originSql;
    }

    public void setOriginSql(String originSql) {
        this.originSql = originSql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
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

    public void setParam(Map<String, Object> param) {
        this.param = param;
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
