import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentManagement {

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

    public void reconcilePayments( Connection database ) {
        Map<Integer, ArrayList<Integer>> map = getUnpaidOrdersWithCustomerId(database);
        for (Map.Entry<Integer, ArrayList<Integer>> entry: map.entrySet()){
            reconcileForCustomer(database, entry.getKey(), entry.getValue());
        }
    }

    private void reconcileForCustomer(Connection database, Integer customerId, ArrayList<Integer> orderNumbers) {
        System.out.println("Customer Number: "+customerId);
        int level = 1;
        ArrayList<Integer> processedOrders = new ArrayList<>();
        while(level <= orderNumbers.size() ) {
            ArrayList<ArrayList<Integer>> combinations = getCombinations(orderNumbers, level);
            for (ArrayList<Integer> combination :
                    combinations) {
                if(hasCommonOrder(combination, processedOrders)) {
                    continue;
                }
                try {
                    Payment payment = OrderPayment.getCheckByCustomerIdAndOrders(database, customerId, combination);
                    if(payment != null) {
                        payOrder(database,payment.amount, payment.check, combination);
                        orderNumbers.removeAll(combination);
                    }
                } catch (SQLException ignored) {
                    return;
                }
            }
            level++;
        }
    }

    private boolean hasCommonOrder(ArrayList<Integer> combination, ArrayList<Integer> processedOrders) {
        for (Integer order:
             processedOrders) {
            if(combination.contains(order)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<ArrayList<Integer>> getCombinations(ArrayList<Integer> orderNumbers, int level) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
        if(level == orderNumbers.size()) {
            combinations.add(orderNumbers);
            return combinations;
        } else if (orderNumbers.size() < level) {
            return combinations;
        }
        for (int i=0; i<=orderNumbers.size()-level; i++) {
            ArrayList<Integer> combination =  new ArrayList<>();
            for (int j = 0; j < level; j++) {
                combination.add(orderNumbers.get(i+j));
            }
            combinations.add(combination);
        }
        return combinations;
    }

    public Map<Integer, ArrayList<Integer>> getUnpaidOrdersWithCustomerId(Connection database) {
        String SQL = "" +
                "SELECT customerNumber, \n" +
                "       Group_concat(orderNumber) AS orderNumbers \n" +
                "FROM   orders \n" +
                "WHERE  payment_status = \"unavailable\" \n" +
                "       AND status NOT IN ( 'Cancelled', 'Disputed' ) \n" +
                "GROUP  BY customerNumber \n" +
                "ORDER  BY shippedDate; ";

        Map<Integer, ArrayList<Integer>> map = new LinkedHashMap<>();
        try {
            ResultSet set = database.prepareStatement(SQL).executeQuery();
            while (set.next()) {
                Integer customerId = set.getInt(1);
                ArrayList<Integer> list = Arrays.stream(set.getString(2)
                        .split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toCollection(ArrayList::new));
                map.put(customerId, list);
            }
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
