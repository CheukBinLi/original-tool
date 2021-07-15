import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.sql.parser.SQLParserUtil;
import com.github.cheukbinli.original.sql.parser.model.MeatdataInfo;
import com.github.cheukbinli.original.sql.parser.model.SQLInfo;
import com.github.cheukbinli.original.sql.parser.model.content.ColumnContent;
import com.github.cheukbinli.original.sql.parser.model.content.ConditionContent;
import com.github.cheukbinli.original.sql.parser.model.content.GroupByContent;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLParserUtilTest {

    public static void main(String[] args) throws Throwable, ClassNotFoundException {

        Class.forName("com.mysql.jdbc.Driver");
//        Class.forName("org.gjt.mm.mysql.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/Atest?characterEncoding=UTF-8";
        Connection conn = DriverManager.getConnection(url, "root", "123456");

        Statement smt = conn.createStatement();
        // 创建表 executeUpdate方法
        String sql1 = "create table  if not exists test14(id int  primary key auto_increment ,name varchar(20),age int);";

        SQLInfo sqlInfo = SQLParserUtil.selectByChooseTable(conn, "SELECT sum(age)x,id FROM test14 WHERE a=#{aa} and b=${aa} group by user", "day", true, CollectionUtil.mapBuilder().append("aa", "99").build());

        System.out.println(sqlInfo.getFinalSQL());
//        SQLParserFactory.create(sql1, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"});

        final AtomicInteger count = new AtomicInteger(0);
        SQLParserUtil.doCreate(conn, sql1, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"}, new SQLParserUtil.SQLParserFactoryListener() {
            @Override
            public String verifyContent(String sql, Object... args) {
                return sql;
            }
        });

//        String sql2 = "SELECT sum(age)x,id FROM test14 where a=11 and b=1 and c=11 or a=12 and a between 1 and 2 order by id";
        String sql2 = "SELECT * FROM test14 order by id";

        Map<String, List<Integer>> result = SQLParserUtil.doSelect(
                conn,
                sql2,
                new MeatdataInfo[]{
                        new MeatdataInfo("day", null, Arrays.asList(new ConditionContent(ConditionContent.Operator.BooleanAnd, "age", ConditionContent.Operator.Equality, "44"))),
                        new MeatdataInfo("year", null, Arrays.asList(new ColumnContent("sum(age)")))
                },
                new MeatdataInfo[]{
                        new MeatdataInfo(
                                "name",
                                null,
                                Arrays.asList(
                                        new GroupByContent(Arrays.asList("name")),
                                        new ConditionContent(ConditionContent.Operator.BooleanAnd, "name", ConditionContent.Operator.Like, "CONCAT('%',#{name},'%')")
                                ),
                                Collections.singletonMap("name", 11)
                        ),
                        new MeatdataInfo("age", null, Arrays.asList(new GroupByContent(Arrays.asList("age")))),
                        new MeatdataInfo("cc", null, Arrays.asList(new GroupByContent(Arrays.asList("cc"))))
                },
                null,
                new SQLParserUtil.DataIterator<Integer>() {
                    @Override
                    public Integer next(Map<String, Object> data, String a, String b, Object params) {
                        Object result = data.get("age");
                        return null == result ? 0 : (Integer) result;
                    }
                }
        );
        System.out.println(result);

        conn.close();

    }

    public interface Executor {
        default void execute(String sql) {
        }

        default void doExecute(Connection connection, String sql) throws SQLException {

            List<SQLInfo> sqlInfoList = SQLParserUtil.create(sql, new String[]{"day", "mon", "year"}, new String[]{"company", "department", "user"});
            for (SQLInfo item : sqlInfoList) {
                PreparedStatement preparedStatement = connection.prepareStatement(item.getFinalSQL());
                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }


}
