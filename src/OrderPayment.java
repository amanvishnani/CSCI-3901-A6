import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class OrderPayment {
    static String SQL_FIND_PAYMENT_ID_BY_CHEQUE = "" +
            "select payment_id from payments where checkNumber = ?";

    static String SQL_INSERT_ONE = "" +
            "insert into order_payment(payment_id, orderNumber) values (? ,?)";

    static String SQL_UPDATE_PAYMENT_STATUS = "" +
            "UPDATE ORDERS " +
            "set payment_status = ? " +
            "where orderNumber in (%s)";
    public static void linkPayment(Connection database, ArrayList<Integer> orders, String chequeNumber) throws SQLException {
        PreparedStatement statement;
        Integer paymentId = findPaymentId(database, chequeNumber);
        if(paymentId<1) {
            return;
        }
        for (Integer orderNumber :
                orders) {
            statement = database.prepareStatement(SQL_INSERT_ONE);
            statement.setInt(1, paymentId);
            statement.setInt(2, orderNumber);
            statement.executeUpdate();
        }
    }

    public static void updateStatus(Connection database, ArrayList<Integer> orders, String status) throws SQLException {
        String SQL = String.format(SQL_UPDATE_PAYMENT_STATUS, QueryUtils.getInClause(orders.size()));
        PreparedStatement statement = database.prepareStatement(SQL);
        statement.setString(1, status);
        QueryUtils.setInClauseParams(statement, orders, 2);
        statement.executeUpdate();
    }

    public static Integer findPaymentId(Connection database, String chequeNumber) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_FIND_PAYMENT_ID_BY_CHEQUE);
        statement.setString(1, chequeNumber);
        Integer paymentId = QueryUtils.getResult(statement, Integer.class);
        if(paymentId == null) {
            return -1;
        }
        return paymentId;
    }
}
