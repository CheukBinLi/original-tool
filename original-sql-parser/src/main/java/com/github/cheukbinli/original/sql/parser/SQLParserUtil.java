package com.github.cheukbinli.original.sql.parser;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.util.JdbcConstants;
import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.common.util.conver.ObjectUtil;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/***
 *
 * @Title: original-sql-parser
 * @Description: SQL解释工具
 * @Company:
 * @Email: cheukbinli@icloud.com
 * @author cheuk.bin.li
 * @date 2021年07月12日 上午9:55:37
 *
 */
public class SQLParserUtil {

    public final static SQLOrderBy SQL_ORDER_BY = new SQLOrderBy();

    private final static String SHOW_TABLES_COMMON = "TABLES";

    private final static String DEFAULT_TABLE_ALIAS_NAME = "ABC";

    final static StringTemplateResourceLoader RESOURCE_LOADER = new StringTemplateResourceLoader();
    static GroupTemplate groupTemplate;

    static {
        try {
            groupTemplate = new GroupTemplate(RESOURCE_LOADER, Configuration.defaultConfiguration());
        } catch (IOException e) {
            groupTemplate = null;
            e.printStackTrace();
        }
    }

    private final static LoadingCache<CacheKey, Set<String>> cacheMap = CacheBuilder
            .newBuilder()
            .initialCapacity(64)
            .maximumSize(1024)
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build(new CacheLoader<CacheKey, Set<String>>() {
                @Override
                public Set<String> load(CacheKey cacheKey) throws Exception {
                    if (SHOW_TABLES_COMMON.equals(cacheKey.getKey()))
                        if (null == cacheKey.getAdditional()) {
                            return null;
                        }
                    Connection connection = cacheKey.getAdditional();
                    PreparedStatement preparedStatement = connection.prepareStatement("SHOW TABLES;");
                    ResultSet resultSet = preparedStatement.executeQuery();
                    Set<String> result = new HashSet<>();
                    while (resultSet.next()) {
                        result.add(resultSet.getString(1));
                    }
                    resultSet.close();
                    preparedStatement.close();
                    return result;
                }
            });

    public static class CacheKey implements Serializable {
        private String key;
        private Object additional;

        public CacheKey(String key, Object additional) {
            this.key = key;
            this.additional = additional;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public <T> T getAdditional() {
            return (T) additional;
        }

        public void setAdditional(Object additional) {
            this.additional = additional;
        }
    }

    static String templateRender(String sql, Map<String, Object> param) {
        param = CollectionUtil.isEmpty(param) ? CollectionUtil.EMPTY_MAP : param;
        Template template = groupTemplate.getTemplate(sql);
        template.binding(param);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        template.renderTo(outputStream);
        return outputStream.toString();
    }

    public static SQLInfo selectByChooseTable(Set<String> tables, String statments, String operationType, boolean cleanGroupBy, Map<String, Object> param) {
        return selectByChooseTable(tables, statments, operationType, null, cleanGroupBy, param);
    }

    public static SQLInfo selectByChooseTable(Connection connection, String statments, String operationType, boolean cleanGroupBy, Map<String, Object> param) {
        return selectByChooseTable(connection, statments, operationType, null, cleanGroupBy, param);
    }

    public static SQLInfo selectByChooseTable(Connection connection, String statments, String operationType, String groupBy, boolean cleanGroupBy, Map<String, Object> param) {
        Set<String> tables = cacheMap.getUnchecked(new CacheKey(SHOW_TABLES_COMMON, connection));
        return selectByChooseTable(tables, statments, operationType, groupBy, cleanGroupBy, param);
    }

    public static SQLInfo selectByChooseTable(Set<String> tables, String statments, String operationType, String groupBy, boolean cleanGroupBy, Map<String, Object> param) {
        statments = templateRender(statments, param);

        StringUtil.StripParam stripParam = StringUtil.stripParam("#{", "}", statments, false);
        statments = stripParam.rebuild("?");
        LinkedHashMap<String, Object> resultParam = null;
        if (CollectionUtil.isNotEmpty(stripParam.getParam())) {
            resultParam = new LinkedHashMap<>();
            for (String paramItem : stripParam.getParam().keySet()) {
                resultParam.put(paramItem, param.get(paramItem));
            }
        }

        List<SQLStatement> statementList = SQLUtils.parseStatements(statments, JdbcConstants.MYSQL);
        for (SQLStatement item : statementList) {
            String tableName, finalTableName, groupByClause = null;
            if (item instanceof SQLSelectStatement) {
                SQLSelectStatement statement = (SQLSelectStatement) item;
                MySqlSelectQueryBlock queryBlock = (MySqlSelectQueryBlock) statement.getSelect().getQuery();
                SQLExprTableSource table = (SQLExprTableSource) queryBlock.getFrom();

                tableName = table.getTableName().replace("`", "");

                if (!StringUtil.isBlank(groupBy) && tables.contains(finalTableName = StringUtil.assemble("_", tableName, operationType, groupBy))) {
                    if (cleanGroupBy) {
                        groupByClause = queryBlock.getGroupBy().toString();
                        queryBlock.setGroupBy(null);
                    }
                    table.setSimpleName(finalTableName);
                    return new SQLInfo(
                            finalTableName,
                            "column",
                            ObjectUtil.defaultNull(queryBlock.getWhere(), SQL_ORDER_BY).toString(),
                            groupByClause,
                            ObjectUtil.defaultNull(queryBlock.getOrderBy(), SQL_ORDER_BY).toString(),
                            statments,
                            queryBlock.toString(),
                            resultParam
                    );
                }


                SQLSelectGroupByClause group = queryBlock.getGroupBy();
                if (null != group) {
                    for (SQLExpr groupItem : group.getItems()) {
                        String itemName;
                        if (groupItem instanceof SQLIdentifierExpr) {
                            itemName = ((SQLIdentifierExpr) groupItem).getName();
                        } else if (groupItem instanceof SQLPropertyExpr) {
                            itemName = ((SQLPropertyExpr) groupItem).getName();
                        } else {
                            break;
                        }
                        if (tables.contains(finalTableName = StringUtil.assemble("_", tableName, operationType, itemName))) {
                            if (cleanGroupBy) {
                                if (group.getItems().size() > 1) {
                                    group.getItems().remove(groupItem);
                                } else {
                                    queryBlock.setGroupBy(null);
                                }
                            }
                            table.setSimpleName(finalTableName);
                            return new SQLInfo(
                                    finalTableName,
                                    "column",
                                    ObjectUtil.defaultNull(queryBlock.getWhere(), SQL_ORDER_BY).toString(),
                                    groupByClause,
                                    ObjectUtil.defaultNull(queryBlock.getOrderBy(), SQL_ORDER_BY).toString(),
                                    statments,
                                    queryBlock.toString(),
                                    resultParam
                            );
                        }
                    }
                }
            }
        }
        return new SQLInfo(statments, statments, resultParam);
    }

    public static List<SQLInfo> select(String statement, MeatdataInfo[] operationTypes, MeatdataInfo[] groupBys, Map<String, Object> param) {
        if (StringUtil.isBlank(statement)) {
            throw new RuntimeException("statement can't be blank");
        }
        statement = templateRender(statement, param);
        List<SQLInfo> result = new ArrayList<>();
        List<SQLStatement> statementList = SQLUtils.parseStatements(statement, JdbcConstants.MYSQL);
        for (SQLStatement item : statementList) {
            String tableName, finalTableName, groupByClause = null;
            if (item instanceof SQLSelectStatement) {
                SQLSelectStatement selectStatement = (SQLSelectStatement) item;
                MySqlSelectQueryBlock queryBlock = (MySqlSelectQueryBlock) selectStatement.getSelect().getQuery();
                SQLExprTableSource table = (SQLExprTableSource) queryBlock.getFrom();

                SQLSelectGroupByClause group = queryBlock.getGroupBy();
                group = null == group ? new SQLSelectGroupByClause() : group;
                group.setParen(true);
                group.setParent(queryBlock);
//                queryBlock.setGroupBy(new SQLSelectGroupByClause());
                queryBlock.setGroupBy(group);

                List<SQLExpr> orginSqlPropertyExprs = new ArrayList<>(group.getItems());

                String tableAlias = StringUtil.isBlank(table.getAlias()) ? null : table.getAlias().replace(".", "");
                if (StringUtil.isBlank(tableAlias)) {
                    table.setAlias(tableAlias = DEFAULT_TABLE_ALIAS_NAME);
                }

                for (MeatdataInfo operationType : operationTypes) {
                    for (MeatdataInfo groupBy : groupBys) {
                        group.getItems().clear();
                        for (String operationTypeValue : operationType.getContent()) {
                            SQLPropertyExpr operationTypePropertyExpr = new SQLPropertyExpr();
                            operationTypePropertyExpr.setName(operationTypeValue);
                            operationTypePropertyExpr.setOwner(tableAlias);

//                            SQLIdentifierExpr sqlIdentifierExpr=new SQLIdentifierExpr();
//                            sqlIdentifierExpr.setName(tableAlias);
//                            sqlIdentifierExpr.setParent(operationTypePropertyExpr);

//                            operationTypePropertyExpr.setOwner(sqlIdentifierExpr);
                            operationTypePropertyExpr.setParent(group);
                            group.getItems().add(operationTypePropertyExpr);
                        }
                        for (String groupByValue : groupBy.getContent()) {
                            SQLPropertyExpr groupByPropertyExpr = new SQLPropertyExpr();
                            groupByPropertyExpr.setName(groupByValue);
                            groupByPropertyExpr.setOwner(tableAlias);
                            groupByPropertyExpr.setParent(group);
                            group.getItems().add(groupByPropertyExpr);
                        }
                        group.getItems().addAll(orginSqlPropertyExprs);

                        StringUtil.StripParam stripParam = StringUtil.stripParam("#{", "}", queryBlock.toString(), false);
                        String sql = stripParam.rebuild("?");
                        LinkedHashMap<String, Object> resultParam = null;
                        if (CollectionUtil.isNotEmpty(stripParam.getParam())) {
                            resultParam = new LinkedHashMap<>();
                            for (String paramItem : stripParam.getParam().keySet()) {
                                resultParam.put(paramItem, param.get(paramItem));
                            }
                        }

                        result.add(new SQLInfo(statement, sql, resultParam));
                    }

                }
            }
        }
        return result;

    }

    public static <T> List<List<T>> doSelect(Connection connection, String statement, MeatdataInfo[] operationTypes, MeatdataInfo[] groupBys, Map<String, Object> param, DataIterator<T> iterator) throws SQLException {
        List<SQLInfo> sqlInfoList = select(statement, operationTypes, groupBys, param);
        List<List<T>> result = new ArrayList<>();
        for (SQLInfo item : sqlInfoList) {

            StringUtil.StripParam stripParam = StringUtil.stripParam("#{", "}", item.getSql(), false);
            String sql = stripParam.rebuild("?");

            PreparedStatement preparedStatement = connection.prepareStatement(item.getSql());

            if (CollectionUtil.isNotEmpty(stripParam.getParamNames())) {
                List<String> paramNames = stripParam.getParamNames();
                for (int i = 0, len = stripParam.getParamNames().size(); i < len; i++) {
                    preparedStatement.setObject(i + 1, param.get(paramNames.get(i)));
                }
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
                columns.add(resultSetMetaData.getColumnName(i + 1));
            }

            T t;
            Map<String, Object> row;
            List<T> subResult = new ArrayList<>();
            int len = columns.size() + (int) (columns.size() * 0.25);
            while (resultSet.next()) {
                row = new HashMap<>(len);
                for (String column : columns) {
                    row.put(column, resultSet.getObject(column));
                }
                if (null == (t = iterator.next(row))) {
                    continue;
                }
                subResult.add(t);
            }
            if (CollectionUtil.isEmpty(subResult)) {
                continue;
            }
            result.add(subResult);
        }
        return result;
    }

    public static List<SQLInfo> create(String statement, String[] operationTypes, String[] groupBys) {
        List<SQLStatement> statementList = SQLUtils.parseStatements(statement, JdbcConstants.MYSQL);
        List<SQLInfo> result = new ArrayList<>();
        for (SQLStatement item : statementList) {
            String tableName = null;
            if (item instanceof MySqlCreateTableStatement) {
                MySqlCreateTableStatement createTableStatement = (MySqlCreateTableStatement) item;
                tableName = createTableStatement.getTableName();
                tableName = tableName.substring(tableName.startsWith("`") ? 1 : 0, tableName.endsWith("`") ? tableName.length() - 1 : tableName.length());

                for (String operationType : operationTypes) {
                    for (String groutBy : groupBys) {
                        createTableStatement.setTableName("`" + StringUtil.assemble("_", tableName, operationType, groutBy) + "`");
                        result.add(new SQLInfo(statement, createTableStatement.toString(), null));
                    }
                }
            }
            return result;
        }
        return result;
    }

    public static int doCreate(Connection connection, String statement, String[] operationTypes, String[] groupBys) throws SQLException {
        return doCreate(connection, statement, operationTypes, groupBys, null);
    }

    public static int doCreate(Connection connection, String statement, String[] operationTypes, String[] groupBys, SQLParserFactoryListener listener) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        listener = null == listener ? SQLParserFactoryListener.DEFAULT_Listener : listener;
        try {
            connection.setAutoCommit(false);

            List<SQLParserUtil.SQLInfo> sqlInfoList = SQLParserUtil.create(statement, operationTypes, groupBys);
            String sql;
            for (SQLParserUtil.SQLInfo item : sqlInfoList) {
                sql = item.getSql();
                sql = listener.doEvent("CREATE", sql).verifyContent(sql);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.execute();
                preparedStatement.close();
            }

            connection.commit();
            return sqlInfoList.size();
        } catch (Throwable e) {
            connection.rollback();
            throw new SQLException(e);
        } finally {
            connection.setAutoCommit(autoCommit);
        }

    }

    public interface SQLParserFactoryListener {

        SQLParserFactoryListener DEFAULT_Listener = new SQLParserFactoryListener() {
        };

        /***
         *
         * @param eventType 事件类型
         * @param sql 执行的SQL
         * @param args SQL参数
         */
        default void event(String eventType, String sql, Object... args) {
        }

        default SQLParserFactoryListener doEvent(String eventType, String sql, Object... args) {
            try {
                event(eventType, sql, args);
            } catch (Throwable e) {
                e.getMessage();
            }
            return this;
        }


        /***
         *
         * @param sql 执行SQL
         * @param args SQL参数
         * @return
         */
        default String verifyContent(String sql, Object... args) {
            return sql;
        }

    }

    public interface DataIterator<T> {
        default T next(Map<String, Object> data) {
            return (T) data;
        }
    }

//    static SQLInfo create(String statement, String... operationType) {
//        List<SQLStatement> statementList = SQLUtils.parseStatements(statement, JdbcConstants.MYSQL);
//        for (SQLStatement item:statementList)
//        if (item instanceof MySqlUpdateStatement) {
//            MySqlUpdateStatement statement = (MySqlUpdateStatement) item;
//        } else if (item instanceof MySqlInsertStatement) {
//            MySqlInsertStatement statement = (MySqlInsertStatement) item;
//        } else if (item instanceof SQLAlterTableStatement) {
//            SQLAlterTableStatement statement = (SQLAlterTableStatement) item;
//        } else if (item instanceof MySqlCreateTableStatement) {
//            MySqlCreateTableStatement statement = (MySqlCreateTableStatement) item;
//        } else {
//            throw new RuntimeException("not support " + item.getClass());
//        }
//    }


    public static void main(String[] args) {
        SQLInfo sqlInfo = selectByChooseTable(new HashSet<>(Arrays.asList("A_day_cc", "A_mon_cc")), "SELECT (Select B.a FROM MMX B) aa,A.* FROM A a WHERE A.id=11 and A.del=0 And A.c=11 group by a.cc;update a set a.a=1 where a.id =11;", "day", true, null);

        List<SQLInfo> sqlInfos = create("CREATE TABLE `travel_company_business` (" +
                "  `id` varchar(32) NOT NULL COMMENT 'id'," +
                "  `company_id` varchar(64) DEFAULT NULL COMMENT '企业id'," +
                "  PRIMARY KEY (`id`)," +
                "  KEY `ids_tcb_company_id` (`company_id`) USING BTREE COMMENT '企业id索引'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='企业商务关系表';", new String[]{"day", "mon", "year"}, new String[]{"A", "B", "C", "D",});
        System.out.println(sqlInfo.getSql());

        for (SQLInfo item : sqlInfos) {
            System.out.println("########################");
            System.out.println(item.getSql());
            System.out.println("########################");
        }

        List<SQLInfo> sqlInfos1 = select("SELECT (Select B.a FROM MMX B) aa,A.* FROM A a WHERE A.id=11 and A.del=0 And A.c=11 group by a.cc",
                new MeatdataInfo[]{new MeatdataInfo("mon", Arrays.asList("mon")),
                        new MeatdataInfo("year", Arrays.asList("year")), new MeatdataInfo("day", Arrays.asList("day"))},
                new MeatdataInfo[]{new MeatdataInfo("XX1", Arrays.asList("bbx"))},
                null);

        for (SQLInfo item : sqlInfos1) {
            System.out.println("************************");
            System.out.println(item.getSql());
            System.out.println("************************");
        }
    }

    public static class MeatdataInfo implements Serializable {

        private static final long serialVersionUID = -6446218504234876544L;

        public enum MetaDataType {COLUMN, CONDITION, GROUP_BY, ORDER_BY}

        public MeatdataInfo(String name, List<String> content) {
            this(name, null, content, null);
        }

        public MeatdataInfo(String name, String alias, List<String> content) {
            this(name, alias, content, null);
        }

        public MeatdataInfo(String name, String alias, List<String> content, MetaDataType metaDataType) {
            this.name = name;
            this.alias = alias;
            this.content = content;
            this.metaDataType = metaDataType;
        }

        /***
         * 表命名键值
         */
        private String name;
        /***
         * 别名
         */
        private String alias;
        /***
         * SQL拼接值
         */
        private List<String> content;
        /***
         * 元素类型
         */
        private MetaDataType metaDataType;
        /***
         * 辅助链
         */
        private MeatdataInfo child;
        /***
         * 辅助链
         */
        private MeatdataInfo parent;


        public String getName() {
            return name;
        }

        public MeatdataInfo setName(String name) {
            this.name = name;
            return this;
        }

        public String getAlias() {
            return alias;
        }

        public MeatdataInfo setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public List<String> getContent() {
            return content;
        }

        public MeatdataInfo setContent(List<String> content) {
            this.content = content;
            return this;
        }

        public MetaDataType getMetaDataType() {
            return metaDataType;
        }

        public MeatdataInfo setMetaDataType(MetaDataType metaDataType) {
            this.metaDataType = metaDataType;
            return this;
        }

        public MeatdataInfo getChild() {
            return child;
        }

        public MeatdataInfo setChild(MeatdataInfo child) {
            this.child = child;
            return this;
        }

        public MeatdataInfo getParent() {
            return parent;
        }

        public MeatdataInfo setParent(MeatdataInfo parent) {
            this.parent = parent;
            return this;
        }
    }

    public static class SQLInfo implements Serializable {

        private static final long serialVersionUID = 3122512942260594875L;
        private String tableName;
        private String columns;
        private String orderBy;
        private String where;
        private String groupBy;
        private String originSql;
        private String sql;
        private Map<String, Object> param;

        public SQLInfo() {
        }

        public SQLInfo(String originSql, String sql, Map<String, Object> param) {
            this.originSql = originSql;
            this.sql = sql;
            this.param = param;
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
    }

}
