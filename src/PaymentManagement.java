import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;

public class PaymentManagement {

    void reconcilePayments(Connection database) {
        DbMigrationUtils.addPaymentStatusColumn(database);
        DbMigrationUtils.createSurrogateKeyInPayments(database);
        DbMigrationUtils.createOrderPaymentsTable(database);
    }

    boolean payOrder(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        return false;
    }

    ArrayList<Integer> unpaidOrders(Connection database) {
        return null;
    }

    ArrayList<String> unknownPayments(Connection database) {
        return null;
    }
}
