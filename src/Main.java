import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {

    public static void main(String[] args) {
        Connection database = ConnectionManager.getConnection();
        PaymentManagement management = new PaymentManagement();
        management.reconcilePayments(database);
        System.out.println(management.unknownPayments(database));
        System.out.println(management.unpaidOrders(database));
        ArrayList<Integer> listOrders = new ArrayList<>(Arrays.asList(10407, 10400));
        System.out.println(management.payOrder(database, 83984.89f, "OP1200", listOrders));
        System.out.println(management.unpaidOrders(database));
    }
}
