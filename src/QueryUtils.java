import java.sql.*;
import java.util.List;

public class QueryUtils {
    public static void startTransaction(Connection connection) throws SQLException {

    }

    public static void rollBackTransaction(Connection connection) throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }


    public static void commitTransaction(Connection connection) throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    public static <T> T getResult(PreparedStatement statement, Class<T> tClass) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            return resultSet.getObject(1, tClass);
        }
        return null;
    }

    public static String getInClause(Integer length) {
        StringBuilder builder = new StringBuilder("");
        for (int i = 0; i < length; i++) {
            builder.append("?");
            if(i!=length-1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public static <T> void setInClauseParams(PreparedStatement statement, List<T> list, Integer startAt) throws SQLException {
        for (T obj :
                list) {
            statement.setObject(startAt, obj);
            startAt = startAt + 1;
        }
    }
}
