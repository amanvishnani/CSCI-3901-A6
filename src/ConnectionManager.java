import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author Aman Vishnani (aman.vishnani@dal.ca)
 *
 * ConnectionManager class provides JDBC conncetion based on provided configuration.
 */
public class ConnectionManager {
    private static String host = "localhost";
    private static String port = "3306";
    private static String db = "csci3901";
    private static String dbParams = "serverTimezone=UTC";
    private static String user = "root";
    private static String password = "root";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = null;
            String connectionStr = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, db, dbParams);
            connection = DriverManager.getConnection(connectionStr, user, password);
            return connection;
        } catch (Exception e) {
            System.out.println("Please configure Connection and add driver to class path.");
            return null;
        }
    }

}
