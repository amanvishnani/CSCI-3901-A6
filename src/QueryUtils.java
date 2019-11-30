import java.sql.*;
import java.util.List;

/**
 * @author Aman Vishnani (aman.vishnani@dal.ca)
 *
 * Utility class for Queries.
 */
public class QueryUtils {

    /**
     * Rollback transaction method.
     * @param connection
     * @throws SQLException
     */
    public static void rollBackTransaction(Connection connection) throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }

    /**
     * Commit transaction.
     * @param connection
     * @throws SQLException
     */
    public static void commitTransaction(Connection connection) throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    /**
     * Gets Single row and Single column results from statement.
     * Mostly used for aggregate functions in SQL.
     * @param statement sql prepared statement.
     * @param tClass class of return type
     * @param <T> generic template
     * @return the object of specified return type
     * @throws SQLException
     */
    public static <T> T getResult(PreparedStatement statement, Class<T> tClass) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            return resultSet.getObject(1, tClass);
        }
        return null;
    }

    /**
     * Creates in clause parameter statement for prepared statement.
     * @param length the number of objects
     * @return string for the in clause
     */
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

    /**
     * Sets parameters in a prepared statement.
     * @param statement the prepared sql statement.
     * @param list the list of objects
     * @param startAt starting position index in statement.
     * @param <T> the type of object
     * @throws SQLException
     */
    public static <T> void setInClauseParams(PreparedStatement statement, List<T> list, Integer startAt) throws SQLException {
        for (T obj :
                list) {
            statement.setObject(startAt, obj);
            startAt = startAt + 1;
        }
    }
}
