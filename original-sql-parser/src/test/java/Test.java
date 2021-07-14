import com.github.cheukbinli.original.sql.parser.SQLParserUtil;
import com.github.cheukbinli.original.sql.parser.model.SQLInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class Test {

    String createTable = "CREATE TABLE if not exists `company_business` (" +
            "`id` VARCHAR ( 32 ) NOT NULL COMMENT 'id'," +
            "`user` VARCHAR ( 64 ) DEFAULT NULL COMMENT '员工'," +
            "`user_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '员工id'," +
            "`company` VARCHAR ( 64 ) DEFAULT NULL COMMENT '企业'," +
            "`company_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '企业id'," +
            "`name` VARCHAR ( 64 ) DEFAULT NULL COMMENT '名称'," +
            "`key` VARCHAR ( 64 ) DEFAULT NULL COMMENT 'key'," +
            "`create` VARCHAR ( 64 ) DEFAULT NULL COMMENT '新增人'," +
            "`create_time` VARCHAR ( 64 ) DEFAULT NULL COMMENT '新增时间'," +
            "PRIMARY KEY ( `id` )," +
            "KEY `ids_tcb_company_id` ( `company_id` ) USING BTREE COMMENT '企业id索引' " +
            ") ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = '企业商务关系表';";

    String select1 = "" +
            "select * from company_business" +
            " where " +
            "1=1 " +
            "<%if(isNotEmpty(key)){print(\" and key=#{key}\");} %> " +
            "and create_time > 'xx' " +
            "group by company_id";

    String select2 = "" +
            "select * from company_business" +
            " where " +
            "1=1 " +
            "<%if(isNotEmpty(key)){print(\" and key=#{key}\");} %> " +
            "and create_time > 'xx' ";

    String select3 = "" +
            "select * from company_business";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
//        new com.mysql.jdbc.Driver();
//        Class.forName("org.gjt.mm.mysql.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/Atest?characterEncoding=UTF-8";
        return DriverManager.getConnection(url, "root", "123456");
    }

    @org.junit.Test
    public void create() throws SQLException, ClassNotFoundException {

        List<SQLInfo> result = SQLParserUtil.create(
                createTable,
                new String[]{"day", "mon", "year"},
                new String[]{"user", "department", "company"}
        );
        result.forEach(item -> System.out.println(item.getSql()+"\n\n"));

    }

    @org.junit.Test
    public void doCreate() throws SQLException, ClassNotFoundException {

        int result = SQLParserUtil.doCreate(
                getConnection(),
                createTable,
                new String[]{"day", "mon", "year"},
                new String[]{"user", "department", "company"},
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


}
