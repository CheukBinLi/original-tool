import com.github.cheukbinli.original.common.util.conver.ObjectFill;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import com.github.cheukbinli.original.sql.parser.SQLParserUtil;
import com.github.cheukbinli.original.sql.parser.model.MeatdataInfo;
import com.github.cheukbinli.original.sql.parser.model.SQLInfo;
import com.github.cheukbinli.original.sql.parser.model.content.ColumnContent;
import com.github.cheukbinli.original.sql.parser.model.content.ConditionContent;
import com.github.cheukbinli.original.sql.parser.model.content.GroupByContent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class Test {

    public String createTable = "CREATE TABLE if not exists `company_business` (" +
            "`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id'," +
            "`user` VARCHAR ( 64 ) DEFAULT NULL COMMENT '员工'," +
            "`user_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '员工id'," +
            "`company` VARCHAR ( 64 ) DEFAULT NULL COMMENT '企业'," +
            "`company_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '企业id'," +
            "`name` VARCHAR ( 64 ) DEFAULT NULL COMMENT '名称'," +
            "`key` bigint DEFAULT 0 COMMENT 'key'," +
            "`create` VARCHAR ( 64 ) DEFAULT NULL COMMENT '新增人'," +
            "`age` int DEFAULT NULL COMMENT '年龄'," +
            "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
            "`year` int DEFAULT 2021 COMMENT '年'," +
            "`mon` int DEFAULT 1 COMMENT '月'," +
            "`day` int DEFAULT 1 COMMENT '日'," +
            "`week` int DEFAULT 1 COMMENT '周'," +
            "PRIMARY KEY (`id`) USING BTREE," +
            "KEY `ids_tcb_company_id` ( `company_id` ) USING BTREE COMMENT '企业id索引' " +
            ") ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = '企业商务关系表';";

    public String getData(int index) {
        return

                new Random().nextInt(99) % 2 == 0 ?
                        "INSERT INTO `company_business`(`user`, `user_id`, `company`, `company_id`, `name`, `age`, `key`, `create`, `create_time`) VALUES ('" + (990 + index) + "', '" + 10010 + index + "', '牛A集团', '666', '用户-" + index + "', " + (new Random().nextInt(10) + 21) + ", " + (new Random().nextInt(5000) + 3500) + ", '" + (990 + index) + "', '2021-07-15 06:45:27');"
                        :
                        "INSERT INTO `company_business`(`user`, `user_id`, `company`, `company_id`, `name`, `age`, `key`, `create`, `create_time`) VALUES ('" + (990 + index) + "', '" + 10010 + index + "', '牛B集团', '777', '用户-" + index + "', " + (new Random().nextInt(10) + 21) + ", " + (new Random().nextInt(5000) + 3500) + ", '" + (990 + index) + "', '2021-07-15 06:45:27');";
    }

    /***
     * 插入SQL改写
     */
    public static String insert = "INSERT INTO `company_business`( `user`, `user_id`, `company`, `company_id`, `name`, `age`, `key`, `create`, `create_time`, `year`, `mon`, `day`, `week`) " +
            "VALUES " +
            "(#{user},#{user_id},#{company},#{company_id},#{name},#{age},#{key},#{create},#{create_time},#{year},#{mon},#{day},#{week});";
    public final static StringUtil.StripParam INSERT = StringUtil.stripParam("#{", "}", insert);

    static {
        insert = INSERT.rebuild("?");
    }


    public String select1 = "" +
            "select * from company_business" +
            " where " +
            "1=1 " +
            "<%if(isNotEmpty(mon)&&mon==true){print(\"and create_time=#{create_time}\");} %> " +
            "<%if(isNotEmpty(user)){print(\"and user=#{user}\");} %> " +
            "group by company";

    public String select2 = "" +
            "select * from company_business" +
            " where " +
            "${AA} " +
            "<%if(isNotEmpty(user)){print(\" and user\"+equals+\"#{user}\");} %> " +
            "and create_time > '2020-08-06 20:19:12' ";

    public String doStatistics = "" +
            "select * from company_business";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
//        new com.mysql.jdbc.Driver();
//        Class.forName("org.gjt.mm.mysql.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/Atest?characterEncoding=UTF-8";
        return DriverManager.getConnection(url, "root", "123456");
    }

    /***
     * 脚本生成例子
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void create() throws SQLException, ClassNotFoundException {

        List<SQLInfo> result = SQLParserUtil.create(
                createTable,
                new String[]{"day", "mon", "year"},
                new String[]{"user", "department", "company"}
        );
        result.forEach(item -> System.out.println(item.getFinalSQL() + "\n\n"));

    }

    /***
     * 脚本生成并执行例子
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void doCreate() throws SQLException, ClassNotFoundException {

        //大表,供例子使用
        getConnection().prepareStatement(createTable).execute();

        for (int i = 0; i < 20; i++) {
            getConnection().prepareStatement(getData(i)).execute();
        }

        int result = SQLParserUtil.doCreate(
                getConnection(),
                createTable,
                new String[]{"day", "mon", "year"},
                new String[]{"user", "age", "department", "company"},
                new SQLParserUtil.SQLParserFactoryListener() {
                    @Override
                    public void event(String eventType, String sql, Object... args) {
                        System.out.println("######################");
                        System.out.println(sql);
                        System.out.println("######################");
                    }
                }
        );
        System.err.println("处理行：" + result);
    }

    /***
     * 脚本生成并执行例子
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void select1() throws SQLException, ClassNotFoundException {

        Map<String, Object> param = new HashMap<>();
        param.put("mon", true);
        SQLInfo select = SQLParserUtil.selectByChooseTable(
                getConnection(),
                select1,
                "mon",
                true,
                param
        );
        System.out.println("完整参数：" + param + "\n初始脚本:" + select1);
        System.out.println("\n最终参与执行参数：" + select.getParam() + "\n执行脚本：\n" + select.getFinalSQL());
    }

    /***
     * 脚本生成并执行例子
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void select2() throws SQLException, ClassNotFoundException {

        Map<String, Object> param = new HashMap<>();
        param.put("user", "711");
        param.put("AA", "1=1");
        param.put("equals", "=");
        SQLInfo select = SQLParserUtil.selectByChooseTable(
                getConnection(),
                select2,
                "day",
                "user",
                true,
                param
        );
        System.out.println("完整参数：" + param + "\n初始脚本:" + select2);
        System.out.println("\n最终参与执行参数：" + select.getParam() + "\n执行脚本：\n" + select.getFinalSQL());
    }

    /***
     * 脚本综合执行例子(日，月，年)
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void doStatistics() throws SQLException, ClassNotFoundException {

        Map<String, Object> monParams = new HashMap<>();
        monParams.put("id", "0");
        monParams.put("AA", "1=1");
        monParams.put("equals", "=");
        monParams.put("startTime", "2021-07-01 00:00:00");
        monParams.put("endTime", "2021-07-31 23:59:59");
        Map<String, List<CompanyBusiness>> result = SQLParserUtil.doSelect(
                getConnection(),
                doStatistics,
                new MeatdataInfo[]{
                        new MeatdataInfo("day", null, null),
                        new MeatdataInfo(
                                "mon",
                                null,
                                Arrays.asList(
                                        new ConditionContent(ConditionContent.Operator.BooleanAnd, "id", ConditionContent.Operator.GreaterThan, "#{id}"),
                                        new ConditionContent(ConditionContent.Operator.BooleanAnd, "create_time", ConditionContent.Operator.GreaterThanOrEqual, "#{startTime}"),
                                        new ConditionContent(ConditionContent.Operator.BooleanAnd, "create_time", ConditionContent.Operator.LessThanOrEqual, "#{endTime}")
                                ),
                                monParams
                        ),
                        new MeatdataInfo("year", null, null),
                },
                new MeatdataInfo[]{
                        new MeatdataInfo("age", null,
                                Arrays.asList(
                                        new ColumnContent("'按年龄统计' as type"),
                                        new GroupByContent(Arrays.asList("age"))
                                )
                        ),
                        new MeatdataInfo(
                                "user",
                                null,
                                Arrays.asList(
                                        new ColumnContent("'按用户统计' as type"),
                                        new GroupByContent(Arrays.asList("user"))
                                )
                        ),
                        new MeatdataInfo(
                                "company",
                                null,
                                Arrays.asList(
                                        new ColumnContent("'按企业统计' as type"),
                                        new GroupByContent(Arrays.asList("company"))
                                ))
                },
                Arrays.asList(new ColumnContent("sum(`key`) as `key1`")),
                null,
                new SQLParserUtil.DataIterator<CompanyBusiness>() {
                    @Override
                    public boolean isExecute(String operationName, String groupName, String sql, Object params) {
                        /***
                         * 过滤不执行日统计
                         */
                        if ("day".equals(groupName)) {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public CompanyBusiness next(Map<String, Object> data, String operationName, String groupName, Object params) {
                        try {
                            /***
                             * 直接插入统计表
                             */
                            data.put("key", data.get("key1"));
                            String sql = insert.replace("`company_business`", "`company_business_" + operationName + "_" + groupName + "`");
                            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
                            int i = 1;
                            for (String item : INSERT.getParamNames()) {
                                preparedStatement.setObject(i++, data.get(item));
                            }
                            preparedStatement.execute();

                            /***
                             * 反序列对象
                             */
                            return ObjectFill.fillObject(CompanyBusiness.class, data);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                },
                new SQLParserUtil.SQLParserFactoryListener() {
                    @Override
                    public void event(String eventType, String sql, Object... args) {
                        System.out.println(args[0] + "\n" + sql + "\n\n");
                    }
                }
        );
        for (Map.Entry<String, List<CompanyBusiness>> entry : result.entrySet()) {
            System.out.println("#########################");
            System.out.println(entry.getKey() + ":  " + entry.getValue().size());
            System.out.println("#########################");
        }
    }


    /***
     * 流程测试
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @org.junit.Test
    public void flow() throws SQLException, ClassNotFoundException {
        //建表、插入测试数据
        doCreate();
        //统计插入
        doStatistics();
    }


}
