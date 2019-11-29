import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Connection database = ConnectionManager.getConnection();
        PaymentManagement management = new PaymentManagement();
        management.reconcilePayments(database);
    }
}
