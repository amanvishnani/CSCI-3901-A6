import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Aman Vishnani (aman.vishnani@dal.ca)
 */
public class PaymentManagement {


    /**
     * Creates a payment record and links them with orders.
     * Validates if Order Value is equal to check amount.
     * Validates if the check number already exists.
     * @param database the connection to the database.
     * @param amount the check amount
     * @param cheque_number the check number
     * @param orders the list of order numbers.
     * @return true if payment was created and linked.
     */
    boolean payOrder(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        if (database == null) return  false;
        if(orders.size()==0) {
            return false;
        }
        Savepoint checkPoint = null;
        try {
            checkPoint = database.setSavepoint();
            boolean v1 = ValidationUtils.validateOrderAmount(database, amount, orders);
            boolean v2 = ValidationUtils.validateChequeNumber(database, cheque_number);
            boolean v3 = ValidationUtils.validateSameCustomer(database, orders);

            /*
            Check if amount does not equals orders value or check already exist or
            orders dont have single distinct customers in db. return false in such case.
             */
            if(!v1 || v2 || !v3) {
                return false;
            }

            database.setAutoCommit(false);

            Integer cstId = OrderPayment.getCustomerIdsForOrders(database, orders).get(0);
            OrderPayment.createCheck(database, cheque_number, amount, cstId);
            linkPayment(database, amount, cheque_number, orders);
            QueryUtils.commitTransaction(database);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if(checkPoint != null) {
                try {
                    QueryUtils.rollBackTransaction(database, checkPoint);
                } catch (SQLException ex) {
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Method to link order and payments.
     * @param database the connection to the database.
     * @param amount the given amount
     * @param cheque_number the given check number
     * @param orders the given list of orders
     * @return true on success.
     */
    private boolean linkPayment(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) throws SQLException {
        if (database == null) return  false;
        if(orders.size()==0) {
            return false;
        }
        boolean valid = ValidationUtils.validateInput(database, amount, cheque_number, orders);
        if (!valid) {
            return false;
        }
        OrderPayment.linkPayment(database, orders, cheque_number);
        System.out.println("[SUCCESS] DB UPDATE. ORDER LINKED WITH PAYMENTS and PAYMENT_STATUS=\"PAID\".");

        return false;
    }

    /**
     * Method to find all unpaid orders.
     * @param database the connection to the database.
     * @return the list of all the orderNumbers which are unpaid.
     */
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

    /**
     * Method to find which payments are not linked to any orders.
     * @param database the connection to the database.
     * @return the list of checkNumbers.
     */
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

    /**
     * Method to find and link payments with relevant orders.
     * @param database the connection to the database.
     */
    public void reconcilePayments( Connection database ) {
        Map<Integer, ArrayList<Integer>> map = getUnpaidOrdersWithCustomerId(database);
        if(map == null) return;
        for (Map.Entry<Integer, ArrayList<Integer>> entry: map.entrySet()){
            reconcileForCustomer(database, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Method to find and link payments with relevant orders for a given customer.
     * @param database the db connection
     * @param customerId the customerId
     * @param orderNumbers the list of all the orderNumber which are unlinked.
     */
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
                Savepoint savepoint = null;
                try {
                    savepoint = database.setSavepoint();
                    database.setAutoCommit(false);
                    Payment payment = OrderPayment.getCheckByCustomerIdAndOrders(database, customerId, combination);
                    if(payment != null) {
                        linkPayment(database,payment.amount, payment.check, combination);
                        orderNumbers.removeAll(combination);
                        processedOrders.addAll(combination);
                    }
                    QueryUtils.commitTransaction(database);
                } catch (SQLException ignored) {
                    try {
                        if(savepoint != null) {
                            QueryUtils.rollBackTransaction(database, savepoint);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
            }
            level++;
        }
    }

    /**
     * Checks if two list has any common orders.
     * @param combination the list 1
     * @param processedOrders the list 2
     * @return true if they have any common orders.
     */
    private boolean hasCommonOrder(ArrayList<Integer> combination, ArrayList<Integer> processedOrders) {
        for (Integer order:
             processedOrders) {
            if(combination.contains(order)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Methods to get n combinations from m possible orderNumbers each combination of k length.
     * where n the total number of orders, m is n-k
     * @param orderNumbers the list of orderNumbers.
     * @param level the k for combinations
     * @return the list of combinations each of length $level.
     */
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

    /**
     * Method to get the a map where key is customerNumber and value is the array list of unpaid orders.
     * @param database the db connection
     * @return the map
     */
    private Map<Integer, ArrayList<Integer>> getUnpaidOrdersWithCustomerId(Connection database) {
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
            e.getMessage();
            return null;
        }
    }
}
