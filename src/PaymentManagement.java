import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class PaymentManagement {

    void reconcilePayments(Connection database) {
        DbMigrationUtils.addPaymentStatusColumn(database);
        DbMigrationUtils.createSurrogateKeyInPayments(database);
        DbMigrationUtils.createOrderPaymentsTable(database);
    }

    boolean payOrder(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        boolean valid = ValidationUtils.validateInput(database, amount, cheque_number, orders);
        if (!valid) {
            return false;
        }
        try {
            QueryUtils.startTransaction(database);
            // @TODO: Fill order_payment table.
            // @TODO: Update payment values.
            QueryUtils.commitTransaction(database);
        } catch (SQLException e) {
            try {
                QueryUtils.rollBackTransaction(database);
            } catch (SQLException ex) {
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
