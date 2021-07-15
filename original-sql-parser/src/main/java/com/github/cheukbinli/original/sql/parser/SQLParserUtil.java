package com.github.cheukbinli.original.sql.parser;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.StringUtils;
import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.common.util.conver.ObjectUtil;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import com.github.cheukbinli.original.sql.parser.model.MeatdataInfo;
import com.github.cheukbinli.original.sql.parser.model.SQLInfo;
import com.github.cheukbinli.original.sql.parser.model.SQLMetaInfo;
import com.github.cheukbinli.original.sql.parser.model.content.BaseContent;
import com.github.cheukbinli.original.sql.parser.model.content.ConditionContent;
import com.github.cheukbinli.original.sql.parser.model.content.GroupByContent;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static final Logger LOG = LoggerFactory.getLogger(SQLParserUtil.class);

    public final static SQLOrderBy SQL_ORDER_BY = new SQLOrderBy();

    private final static String SHOW_TABLES_COMMON = "TABLES";

    private final static String DEFAULT_TABLE_ALIAS_NAME = "ABC";

    private final static DataIterator DEFAULT_DATA_ITERATOR = new DataIterator() {
    };

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

        List<SQLStatement> statementList = SQLUtils.parseStatements(statments, JdbcConstants.MYSQL);
        for (SQLStatement item : statementList) {
            String tableName, finalTableName, groupByClause = null;
            if (item instanceof SQLSelectStatement) {
                SQLSelectStatement statement = (SQLSelectStatement) item;
                MySqlSelectQueryBlock queryBlock = (MySqlSelectQueryBlock) statement.getSelect().getQuery();
                SQLExprTableSource table = (SQLExprTableSource) queryBlock.getFrom();

                tableName = table.getName().getSimpleName().replace("`", "");

                if (!StringUtil.isBlank(groupBy) && tables.contains(finalTableName = StringUtil.assemble("_", tableName, operationType, groupBy))) {
                    if (cleanGroupBy && null != queryBlock.getGroupBy()) {
                        groupByClause = queryBlock.getGroupBy().toString();
                        queryBlock.setGroupBy(null);
                    }

//                    table.setSimpleName(finalTableName);
                    setName(table.getName(), finalTableName);
                    String rebuildSQL = queryBlock.toString();
                    SQLInfo finalSQL = buildFinalSQL(rebuildSQL, param);
                    return new SQLInfo(
                            finalTableName,
                            null,
                            ObjectUtil.defaultNull(queryBlock.getWhere(), SQL_ORDER_BY).toString(),
                            groupByClause,
                            ObjectUtil.defaultNull(queryBlock.getOrderBy(), SQL_ORDER_BY).toString(),
                            statments,
                            rebuildSQL,
                            finalSQL.getFinalSQL(),
                            finalSQL.getParam()
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
//                            table.setSimpleName(finalTableName);
                            setName(table.getName(), finalTableName);
                            String rebuildSQL = queryBlock.toString();
                            SQLInfo finalSQL = buildFinalSQL(rebuildSQL, param);
                            return new SQLInfo(
                                    finalTableName,
                                    null,
                                    ObjectUtil.defaultNull(queryBlock.getWhere(), SQL_ORDER_BY).toString(),
                                    groupByClause,
                                    ObjectUtil.defaultNull(queryBlock.getOrderBy(), SQL_ORDER_BY).toString(),
                                    statments,
                                    rebuildSQL,
                                    finalSQL.getFinalSQL(),
                                    finalSQL.getParam()
                            );
                        }
                    }
                }
            }
        }
        return new SQLInfo(statments, statments, statments, param);
    }

    public static List<SQLInfo> select(String statement, MeatdataInfo[] operationTypes, MeatdataInfo[] groupBys, List<BaseContent> baseContents, Map<String, Object> param) {
        if (StringUtil.isBlank(statement)) {
            throw new RuntimeException("statement can't be blank");
        }

        List<SQLInfo> result = new ArrayList<>();
        List<SQLStatement> statementList = SQLUtils.parseStatements(statement, JdbcConstants.MYSQL);

        for (SQLStatement item : statementList) {
            String tableName = null;
            SQLMetaInfo metaInfo;
            if (item instanceof SQLSelectStatement) {
                SQLSelectStatement selectStatement = (SQLSelectStatement) item;
                MySqlSelectQueryBlock queryBlock = (MySqlSelectQueryBlock) selectStatement.getSelect().getQuery();

                SQLExprTableSource table = (SQLExprTableSource) queryBlock.getFrom();
                String tableAlias = StringUtil.isBlank(table.getAlias()) ? null : table.getAlias().replace(".", "");
                if (StringUtil.isBlank(tableAlias)) {
                    table.setAlias(tableAlias = DEFAULT_TABLE_ALIAS_NAME);
                }

                tableName = table.getName().getSimpleName().replace("`", "");
                metaInfo = new SQLMetaInfo(tableName, tableAlias);

                queryBlock = processHandler(queryBlock, true, metaInfo, baseContents);

                for (MeatdataInfo operationType : operationTypes) {

                    MySqlSelectQueryBlock newQueryBlock = processHandler(queryBlock, false, metaInfo, null);
                    SQLSelectGroupByClause group = newQueryBlock.getGroupBy();
                    group = null == group ? new SQLSelectGroupByClause() : group;

                    group.setParent(newQueryBlock);
                    newQueryBlock.setGroupBy(group);
                    newQueryBlock = processHandler(newQueryBlock, true, metaInfo, operationType.getContent());
                    List<SQLExpr> orginSqlPropertyExprs = new ArrayList<>(group.getItems());

                    List<SQLSelectItem> orginColumns = new ArrayList<>(newQueryBlock.getSelectList());
                    SQLExpr orginWhere = newQueryBlock.getWhere();


                    for (MeatdataInfo groupBy : groupBys) {
                        group.getItems().clear();
                        if (CollectionUtil.isNotEmpty(orginSqlPropertyExprs)) {
                            group.getItems().addAll(orginSqlPropertyExprs);
                        }
                        newQueryBlock.setWhere(null == orginWhere ? null : orginWhere.clone());
                        newQueryBlock.getSelectList().clear();
                        newQueryBlock.getSelectList().addAll(orginColumns);

                        if (CollectionUtil.isNotEmpty(groupBy.getContent())) {
                            processHandler(newQueryBlock, true, metaInfo, groupBy.getContent());
                        }

                        Map<String, Object> currentParam = CollectionUtil.collage(param, operationType.getAdditionalParams(), groupBy.getAdditionalParams());
                        //SQL重建
                        statement = templateRender(newQueryBlock.toString(), currentParam);
                        String rebuildSQL = newQueryBlock.toString();
                        SQLInfo finalSQL = buildFinalSQL(rebuildSQL, currentParam);

                        result.add(new SQLInfo(statement, rebuildSQL, finalSQL.getFinalSQL(), operationType.getName(), groupBy.getName(), finalSQL.getParam()));
                    }

                }
            }
        }
        return result;

    }

    /***
     *
     * @param connection 数据连接
     * @param statement 原sql
     * @param operationTypes 统计参数集1
     * @param groupBys 统计参数集2
     * @param param 查询参数
     * @param iterator 迭代器返回null时，结果集不与收录（降低内存使用量）
     * @param <T>
     * @return
     * @throws SQLException
     */
    public static <T> Map<String, List<T>> doSelect(Connection connection, String statement, MeatdataInfo[] operationTypes, MeatdataInfo[] groupBys, Map<String, Object> param, DataIterator<T> iterator) throws SQLException {
        return doSelect(connection, statement, operationTypes, groupBys, null, param, iterator, null);
    }

    /***
     *
     * @param connection 连接dataSource.getConnection()
     * @param statement SQL
     * @param operationTypes 分组规则1
     * @param groupBys 分组规则2
     * @param baseContents 公共变量
     * @param param 执行参数
     * @param iterator 记录迭代器
     * @param listener 执行监听器
     * @param <T>
     * @return
     * @throws SQLException
     */
    public static <T> Map<String, List<T>> doSelect(Connection connection, String statement, MeatdataInfo[] operationTypes, MeatdataInfo[] groupBys, List<BaseContent> baseContents, Map<String, Object> param, DataIterator<T> iterator, SQLParserFactoryListener listener) throws SQLException {
        iterator = null == iterator ? DEFAULT_DATA_ITERATOR : iterator;
        List<SQLInfo> sqlInfoList = select(statement, operationTypes, groupBys, baseContents, param);
        Map<String, List<T>> result = new HashMap<>();
        for (SQLInfo item : sqlInfoList) {
            if (!iterator.isExecute(item.getOperationName(), item.getGroupName(), item.getFinalSQL(), item.getParam())) {
                continue;
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(item.getFinalSQL());
            }

            PreparedStatement preparedStatement = connection.prepareStatement(item.getFinalSQL());
            if (null != listener) {
                listener.doEvent("SELECT", item.getFinalSQL(), item.getParam());
            }
            if (CollectionUtil.isNotEmpty(item.getParam())) {
                int i = 0;
                for (Map.Entry<String, Object> entry : item.getParam().entrySet()) {
                    preparedStatement.setObject(++i, entry.getValue());
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
                if (null == (t = iterator.next(row, item.getOperationName(), item.getGroupName(), preparedStatement.getParameterMetaData()))) {
                    continue;
                }
                subResult.add(t);
            }
//            if (CollectionUtil.isEmpty(subResult)) {
//                continue;
//            }
            result.put(item.getOperationName() + "_" + item.getGroupName(), subResult);
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
                tableName = createTableStatement.getName().getSimpleName();
                tableName = tableName.substring(tableName.startsWith("`") ? 1 : 0, tableName.endsWith("`") ? tableName.length() - 1 : tableName.length());

                for (String operationType : operationTypes) {
                    for (String groutBy : groupBys) {
//                        createTableStatement.setTableName("`" + StringUtil.assemble("_", tableName, operationType, groutBy) + "`");
                        setName(createTableStatement.getName(), "`" + StringUtil.assemble("_", tableName, operationType, groutBy) + "`");
                        result.add(new SQLInfo(statement, null, createTableStatement.toString(), null));
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

    /***
     *
     * @param connection 连接dataSource.getConnection()
     * @param statement  SQL
     * @param operationTypes 分组类型1
     * @param groupBys 分组类型2
     * @param listener 事件侦听
     * @return
     * @throws SQLException
     */
    public static int doCreate(Connection connection, String statement, String[] operationTypes, String[] groupBys, SQLParserFactoryListener listener) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        listener = null == listener ? SQLParserFactoryListener.DEFAULT_Listener : listener;
        try {
            connection.setAutoCommit(false);

            List<SQLInfo> sqlInfoList = SQLParserUtil.create(statement, operationTypes, groupBys);
            String sql;
            for (SQLInfo item : sqlInfoList) {
                sql = item.getFinalSQL();
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

    static MySqlSelectQueryBlock processHandler(MySqlSelectQueryBlock queryBlock, boolean isSingleInstance, SQLMetaInfo metaInfo, List<BaseContent> contents) {
        if (null == queryBlock) {
            throw new NullPointerException("queryBlock can't be null.");
        }
        MySqlSelectQueryBlock result = isSingleInstance ? queryBlock : queryBlock.clone();
        if (CollectionUtil.isEmpty(contents)) {
            return result;
        }
        if (null == metaInfo || StringUtil.isBlank(metaInfo.getTableName())) {
            SQLExprTableSource table = (SQLExprTableSource) result.getFrom();
            String tableAlias = StringUtil.isBlank(table.getAlias()) ? null : table.getAlias().replace(".", "");
            if (StringUtil.isBlank(tableAlias)) {
                table.setAlias(tableAlias = DEFAULT_TABLE_ALIAS_NAME);
            }
            String tableName = table.getName().getSimpleName().replace("`", "");
            metaInfo = new SQLMetaInfo(tableName, tableAlias);
        }

        for (BaseContent contentItem : contents) {
            switch (contentItem.getType()) {
                case COLUMN:
                    List<SQLSelectItem> columns = result.getSelectList();
                    String column = (String) contentItem.getValue();
                    if (StringUtil.isBlank(column)) {
                        break;
                    }
                    columns.add(new SQLSelectItem(new SQLIdentifierExpr(column)));
                    break;
                case CONDITION:
                    ConditionContent condition = (ConditionContent) contentItem;
                    if (null == condition) {
                        break;
                    }
                    SQLBinaryOpExpr where = (SQLBinaryOpExpr) result.getWhere();

                    SQLIdentifierExpr key = new SQLIdentifierExpr(condition.getName());
                    SQLIdentifierExpr value = new SQLIdentifierExpr(condition.getValue());
                    SQLBinaryOpExpr link = new SQLBinaryOpExpr(key, SQLBinaryOperator.valueOf(condition.getOperator().toString()), value);

                    result.setWhere(null == where ? link : new SQLBinaryOpExpr(where, SQLBinaryOperator.valueOf(condition.getLeftOperator().toString()), link));

                    break;
                case GROUP_BY:
                    GroupByContent groupBy = (GroupByContent) contentItem;
                    if (CollectionUtil.isEmpty(groupBy)) {
                        break;
                    }
//                    SQLSelectGroupByClause parent = null != metaInfo && metaInfo.getParent() instanceof SQLSelectGroupByClause ? metaInfo.getParent() : result.getGroupBy();
                    SQLSelectGroupByClause parent = result.getGroupBy();
                    if (null == parent) {
                        parent = new SQLSelectGroupByClause();
                        parent.setParent(result);
                        result.setGroupBy(parent);
                    }
                    for (String groupByValue : groupBy.getValue()) {
                        SQLPropertyExpr groupByPropertyExpr = new SQLPropertyExpr();
                        groupByPropertyExpr.setName(groupByValue);
                        groupByPropertyExpr.setOwner(metaInfo.getAliasName());
                        groupByPropertyExpr.setParent(parent);
                        parent.getItems().add(groupByPropertyExpr);
                    }
                    break;
                case ORDER_BY:
                    break;
                default:
                    break;
            }
//            processHandler(result, true, metaInfo, metaItem.getChild());
        }
        return result;

    }

    static SQLInfo buildFinalSQL(String sqlStr, Map<String, Object> params) {
        StringUtil.StripParam stripParam = StringUtil.stripParam("#{", "}", sqlStr, false);
        String sql = stripParam.rebuild("?");
        Map<String, Object> resultParam = Collections.EMPTY_MAP;
        if (CollectionUtil.isNotEmpty(stripParam.getParam())) {
            resultParam = new LinkedHashMap<>();
            for (String paramItem : stripParam.getParam().keySet()) {
                resultParam.put(paramItem, params.get(paramItem));
            }
        }
        return new SQLInfo().setFinalSQL(sql).setParam(resultParam);
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

        /***
         *  判断是否执行当前SQL
         * @param operationName 分组类型1
         * @param groupName 分组类型2
         * @param sql 执行SQL
         * @param params 参数
         * @return
         */
        default boolean isExecute(String operationName, String groupName, String sql, Object params) {
            return true;
        }

        /***
         *
         *  结果集迭代器
         *
         * @param rowData 行数据
         * @param operationName 分组类型1
         * @param groupName 分组类型2
         * @param params 执行参数
         * @return
         */
        default T next(Map<String, Object> rowData, String operationName, String groupName, Object params) {
            return (T) rowData;
        }
    }

    static void setName(SQLName sqlName, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("schema is empty.");
        } else {
            if (sqlName instanceof SQLPropertyExpr) {
                ((SQLPropertyExpr) sqlName).setName(name);
            } else {
                ((SQLIdentifierExpr) sqlName).setName(name);
            }
        }
    }

    public static void main(String[] args) {
        SQLInfo sqlInfo = selectByChooseTable(new HashSet<>(Arrays.asList("A_day_cc", "A_mon_cc")), "SELECT (Select B.a FROM MMX B) aa,A.* FROM A a WHERE A.id=11 and A.del=0 And A.c=11 group by a.cc;update a set a.a=1 where a.id =11;", "day", true, null);

        List<SQLInfo> sqlInfos = create("CREATE TABLE `travel_company_business` (" +
                "  `id` varchar(32) NOT NULL COMMENT 'id'," +
                "  `company_id` varchar(64) DEFAULT NULL COMMENT '企业id'," +
                "  PRIMARY KEY (`id`)," +
                "  KEY `ids_tcb_company_id` (`company_id`) USING BTREE COMMENT '企业id索引'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='企业商务关系表';", new String[]{"day", "mon", "year"}, new String[]{"A", "B", "C", "D",});
        System.out.println(sqlInfo.getFinalSQL());

        for (SQLInfo item : sqlInfos) {
            System.out.println("########################");
            System.out.println(item.getFinalSQL());
            System.out.println("########################");
        }

//        List<SQLInfo> sqlInfos1 = select("SELECT (Select B.a FROM MMX B) aa,A.* FROM A a WHERE A.id=11 and A.del=0 And A.c=11 group by a.cc",
//                new MeatdataInfo[]{new MeatdataInfo("mon", Arrays.asList("mon")),
//                        new MeatdataInfo("year", Arrays.asList("year")), new MeatdataInfo("day", Arrays.asList("day"))},
//                new MeatdataInfo[]{new MeatdataInfo("XX1", Arrays.asList("bbx"))},
//                null);
//
//        for (SQLInfo item : sqlInfos1) {
//            System.out.println("************************");
//            System.out.println(item.getSql());
//            System.out.println("************************");
//        }
    }


}
