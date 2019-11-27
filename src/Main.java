import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Connection database = ConnectionManager.getConnection();
        PaymentManagement management = new PaymentManagement();
        management.reconcilePayments(database);
        Integer[] orders = {10298};
        management.payOrder(database, 6066.78f, "HQ336336", new ArrayList<>(Arrays.asList(orders)));
        System.out.println(management.unpaidOrders(database));
        System.out.println(management.unknownPayments(database));
    }
}
