import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class OrderPayment {
    static String SQL_FIND_PAYMENT_ID_BY_CHEQUE = "" +
            "select payment_id from payments where checkNumber = ?";

    static String SQL_UPDATE_ORDERS = "" +
            "update orders set payment_id = ?, payment_status = ? where orderNumber in (%s)";

    public static void linkPayment(Connection database, ArrayList<Integer> orders, String chequeNumber) throws SQLException {
        PreparedStatement statement;
        Integer paymentId = findPaymentId(database, chequeNumber);
        if(paymentId<1) {
            return;
        }
        String SQL = String.format(SQL_UPDATE_ORDERS, QueryUtils.getInClause(orders.size()));
        statement = database.prepareStatement(SQL);
        statement.setInt(1, paymentId);
        statement.setString(2, "PAID");
        QueryUtils.setInClauseParams(statement, orders, 3);
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
