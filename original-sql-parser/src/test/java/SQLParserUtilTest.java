import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.sql.parser.SQLParserUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLParserUtilTest {

    public static void main(String[] args) throws Throwable, ClassNotFoundException {

//        Class.forName("com.mysql.jdbc.Driver");
        Class.forName("org.gjt.mm.mysql.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/Atest?characterEncoding=UTF-8";
        Connection conn = DriverManager.getConnection(url, "root", "123456");

        Statement smt = conn.createStatement();
        // 创建表 executeUpdate方法
        String sql1 = "create table  if not exists test14(id int  primary key auto_increment ,name varchar(20),age int);";

        SQLParserUtil.SQLInfo sqlInfo = SQLParserUtil.selectByChooseTable(conn, "SELECT sum(age)x,id FROM test14 WHERE a=#{aa} and b=${aa} group by user", "day", true, CollectionUtil.mapBuilder().append("aa","99").build());

        System.out.println(sqlInfo.getSql());
//        SQLParserFactory.create(sql1, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"});

        final AtomicInteger count = new AtomicInteger(0);
        SQLParserUtil.doCreate(conn, sql1, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"}, new SQLParserUtil.SQLParserFactoryListener() {
            @Override
            public String verifyContent(String sql, Object... args) {
                return sql;
            }
        });

        String sql2 = "SELECT sum(age)x,id FROM test14";

        Object o = SQLParserUtil.doSelect(
                conn,
                sql2,
                new SQLParserUtil.MeatdataInfo[]{new SQLParserUtil.MeatdataInfo("age", Arrays.asList("age"))},
                new SQLParserUtil.MeatdataInfo[]{new SQLParserUtil.MeatdataInfo("name", Arrays.asList("name")), new SQLParserUtil.MeatdataInfo("cc", Arrays.asList("cc"))},
                null,
                new SQLParserUtil.DataIterator<Object>() {

                    @Override
                    public Object next(Map<String, Object> data) {
                        return data;
                    }
                }
        );
        System.out.println(1);

//
//        new Executor() {
//        }.doExecute(conn, sql1);


//        smt.executeUpdate(sql1);

//        //  插入数据
//        String sql_i = "insert into  test14 values(1,'刘备',45),(2,'关羽',40),(3,'张飞',37),(4,'赵云',30),(5,'诸葛亮',27);";
//        smt.executeUpdate(sql_i);
//
//        // 更新数据
//        String sql_u= "update  test14 set age = 36 where name='张飞';";
//        smt.executeUpdate(sql_u);
//
//        // 查询结果
//        String  sql_q = "select  * from  test14;";
//        ResultSet res = smt.executeQuery(sql_q);
//        while(res.next()){
//            int    id = res.getInt(1);
//            String  name= res.getString("name");
//            int  age = res.getInt("age");
//            System.out.println("id:"+ id + "    name:" + name +"    age:"+age);
//        }


        // 关闭流 (先开后关)
//        res.close();
//        smt.close();
        conn.close();

    }

    public interface Executor {
        default void execute(String sql) {
        }

        default void doExecute(Connection connection, String sql) throws SQLException {

            List<SQLParserUtil.SQLInfo> sqlInfoList = SQLParserUtil.create(sql, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"});
            for (SQLParserUtil.SQLInfo item : sqlInfoList) {
                PreparedStatement preparedStatement = connection.prepareStatement(item.getSql());
                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }


}
