import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class PaymentManagement {

    void reconcilePayments(Connection database) {
        DbMigrationUtils.addPaymentStatusColumn(database);
        DbMigrationUtils.createSurrogateKeyInPayments(database);
        DbMigrationUtils.addPaymentIdInOrdersTable(database);
    }

    boolean payOrder(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        if (database == null) return  false;
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
            System.out.println("[SUCCESS] DB UPDATE. ORDER LINKED WITH PAYMENTS and PAYMENT_STATUS=\"PAID\".");
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
        if (database == null) {
            return null;
        }
        String SQL = "" +
                "select distinct orderNumber " +
                "from orders " +
                "where payment_status!='PAID' and status not in ('Cancelled', 'Disputed')";
        ArrayList<Integer> list = new ArrayList<>();
        try {
            ResultSet set = database.prepareStatement(SQL).executeQuery();
            while (set.next()) {
                list.add(set.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    ArrayList<String> unknownPayments(Connection database) {
        if(database == null) {
            return null;
        }
        String SQL = "" +
                "select checkNumber " +
                "from payments " +
                "where payment_id not in " +
                "(select distinct payment_id from orders where payment_id is not null)";
        ArrayList<String> list = new ArrayList<>();
        try {
            ResultSet set = database.prepareStatement(SQL).executeQuery();
            while (set.next()) {
                list.add(set.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
