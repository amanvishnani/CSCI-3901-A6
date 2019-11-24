import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryUtils {
    public static void startTransaction(Connection connection) throws SQLException {
        String SQL_TURN_OFF_AUTO_COMMIT = "SET autocommit = OFF";
        String SQL_START_TRANSACTION = "START TRANSACTION";
        Statement statement = connection.createStatement();
        statement.executeUpdate(SQL_TURN_OFF_AUTO_COMMIT);
        statement.executeUpdate(SQL_START_TRANSACTION);
    }

    public static void rollBackTransaction(Connection connection) throws SQLException {
        String SQL_ROLLBACK = "ROLLBACK";
        Statement statement = connection.createStatement();
        statement.executeUpdate(SQL_ROLLBACK);
        turnOnAutoCommit(connection);
    }

    public static void turnOnAutoCommit(Connection connection) throws SQLException {
        String SQL_TURN_ON_AUTO_COMMIT = "SET autocommit = ON";
        Statement statement = connection.createStatement();
        statement.executeUpdate(SQL_TURN_ON_AUTO_COMMIT);
    }

    public static void commitTransaction(Connection connection) throws SQLException {
        String SQL_COMMIT = "COMMIT";
        Statement statement = connection.createStatement();
        statement.executeUpdate(SQL_COMMIT);
        turnOnAutoCommit(connection);
    }
}
