import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Utility class for Orders and Payments modifications.
 *
 * @author Aman Vishnani (aman.vishnani@dal.ca)
 */
public class OrderPayment {
    /*
    SQL to find Payment Id by Check Number
     */
    static String SQL_FIND_PAYMENT_ID_BY_CHEQUE = "" +
            "select payment_id from payments where checkNumber = ?";

    /*
    SQL to update payment payment details and payment status in order table.
     */
    static String SQL_UPDATE_ORDERS = "" +
            "update orders set payment_id = ?, payment_status = ? where orderNumber in (%s)";

    /**
     * Method to link orders and payments with order number and checkNumber.
     * @param database the connection to the database.
     * @param orders the list of order numbers to link a payment with.
     * @param chequeNumber the check number from payments table.
     * @throws SQLException
     */
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

    /**
     * Method to find payment_id from check number.
     * PaymentId is generated during database migration.
     * @param database the connection to the database.
     * @param chequeNumber the check number from payments table.
     * @return the payment_id
     * @throws SQLException
     */
    public static Integer findPaymentId(Connection database, String chequeNumber) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_FIND_PAYMENT_ID_BY_CHEQUE);
        statement.setString(1, chequeNumber);
        Integer paymentId = QueryUtils.getResult(statement, Integer.class);
        if(paymentId == null) {
            return -1;
        }
        return paymentId;
    }

    /**
     * Method to find the Payment(CheckNumber, Amount) which matches the sum of all the order amounts.
     * CustomerId is required as this will also assert whether the provided orderNumber belongs to same customers.
     * @param database the connection to the database.
     * @param customerId the single customerId of all the orders.
     * @param orders the list of order numbers.
     * @return the Payment Object if found else null.
     * @throws SQLException
     */
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
                ") and paymentDate >= (select min(o.orderDate) from orders as o where o.orderNumber in (%s))";

        String inClause = QueryUtils.getInClause(orders.size());
        SQL = String.format(SQL, inClause, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        statement.setInt(1, customerId);
        QueryUtils.setInClauseParams(statement, orders, 2);
        QueryUtils.setInClauseParams(statement, orders, 2 + orders.size());
        ResultSet set = statement.executeQuery();
        if(set.next()) {
            return new Payment(set.getString(1), set.getFloat(2));
        }
        return null;
    }
}

/**
 * The payment helper class.
 */
class Payment {
    public String check;
    public Float amount;

    public Payment(String check, Float amount) {
        this.check = check;
        this.amount = amount;
    }
}
