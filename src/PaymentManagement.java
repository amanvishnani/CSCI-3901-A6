import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class PaymentManagement {

    void reconcilePayments(Connection database) {
        DbMigrationUtils.addPaymentStatusColumn(database);
        DbMigrationUtils.createSurrogateKeyInPayments(database);
        DbMigrationUtils.createOrderPaymentsTable(database);
    }

    boolean payOrder(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        if(orders.size()==0) {
            return false;
        }
        boolean valid = ValidationUtils.validateInput(database, amount, cheque_number, orders);
        if (!valid) {
            return false;
        }
        try {
            database.setAutoCommit(false);
            OrderPayment.linkPayment(database, orders, cheque_number);
            System.out.println("[SUCCESS] DB UPDATE. ORDER LINKED WITH PAYMENTS.");
            OrderPayment.updateStatus(database, orders, "PAID");
            System.out.println("[SUCCESS] DB UPDATE. ORDER PAYMENT_STATUS=\"PAID\".");
            QueryUtils.commitTransaction(database);
        } catch (SQLException e) {
            System.out.println("[FAILED] DB UPDATE. ROLLBACK.");
            try {
                QueryUtils.rollBackTransaction(database);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
            return false;
        }
        return false;
    }

    ArrayList<Integer> unpaidOrders(Connection database) {
        return null;
    }

    ArrayList<String> unknownPayments(Connection database) {
        return null;
    }
}
