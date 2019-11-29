import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    static Payment getCheckByCustomerIdAndOrders(Connection database, Integer customerId, ArrayList<Integer> orders) throws SQLException {
        Integer paymentId = null;
        String SQL = "" +
                "SELECT checkNumber, amount \n" +
                "from payments \n" +
                "where customerNumber=? and " +
                "payment_id not in (select distinct payment_id from orders where payment_id is not null) and " +
                "amount = ( " +
                "   select sum(od.quantityOrdered*od.priceEach) " +
                "   from orders as o " +
                "   natural join orderDetails as od " +
                "   where o.orderNumber in (%s) " +
                "   and o.payment_status != 'PAID'" +
                ")";

        String inClause = QueryUtils.getInClause(orders.size());
        SQL = String.format(SQL, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        statement.setInt(1, customerId);
        QueryUtils.setInClauseParams(statement, orders, 2);
        ResultSet set = statement.executeQuery();
        if(set.next()) {
            return new Payment(set.getString(1), set.getFloat(2));
        }
        return null;
    }
}

class Payment {
    public String check;
    public Float amount;

    public Payment(String check, Float amount) {
        this.check = check;
        this.amount = amount;
    }
}
