import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for validation.
 * @author Aman Vishnani (aman.vishnani@dal.ca)
 */
public class ValidationUtils {
    /*
    SQL to find weather or not check number exists.
     */
    static final String SQL_FIND_PAYMENT_BY_CHEQUE_NO = "" +
            "select count(*) from payments\n" +
            "where checkNumber = ?;";

    /*
    SQL to check if all order number exists.
     */
    static final String SQL_FIND_ORDER_BY_ORDER_NUMBER = "" +
            "select count(*) from orders\n" +
            "where orderNumber in (%s);";

    /*
    SQL to find sum of all the mentioned order's amount
     */
    static final String SQL_SUM_ORDERS_AMOUNT = "" +
            "select sum(od.quantityOrdered * od.priceEach)\n" +
            "from orders as o\n" +
            "natural join orderdetails as od\n" +
            "where o.orderNumber in (%s)\n";
    /*
    SQL to find Check Amount by check number.
     */
    static final String SQL_GET_CHEQUE_AMOUNT = "" +
            "select amount from payments\n" +
            "where checkNumber = ?;";

    /*
    SQL to check if check number belongs to same customer as the orders belong to.
     */
    static final String SQL_CHECK_CUSTOMER_VALIDATION = "" +
            "select orderNumber from orders\n" +
            "where orderNumber in (%s)\n" +
            "and customerNumber not in (select customerNumber from  payments where checkNumber = ?)";

    /**
     * Validates all the input rules according to Business rules.
     * @param database the connection to the database.
     * @param amount the total amount paid.
     * @param cheque_number the check number.
     * @param orders the list of order numbers.
     * @return true if the input is valid.
     */
    public static boolean validateInput(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) {
        try {
            return  validateChequeNumber(database, cheque_number) &&
                    validateOrderNumbers(database, orders) &&
                    validateOrderAmount(database, amount, orders) &&
                    validateSameCustomer(database, cheque_number, orders) &&
                    validateChequeAmount(database, amount, cheque_number, orders)
                    ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

    /**
     * Method to validate if the orders belong to same customers.
     * @param database the connection to the database.
     * @param cheque_number the check number.
     * @param orders the list of order numbers.
     * @return true if the input is valid.
     * @throws SQLException
     */
    public static boolean validateSameCustomer(Connection database, String cheque_number, ArrayList<Integer> orders) throws SQLException {
        String inClause = QueryUtils.getInClause(orders.size());
        String SQL = String.format(SQL_CHECK_CUSTOMER_VALIDATION, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        QueryUtils.setInClauseParams(statement, orders, 1);
        statement.setString(orders.size()+1, cheque_number);
        ResultSet set = statement.executeQuery();
        if(set.next()) {
            System.out.println("Assertion Error: Order's Customer and Cheque's Customer dont match.");
            return false;
        }
        return true;
    }

    public static boolean validateSameCustomer(Connection database, ArrayList<Integer> orders) throws SQLException {
        ArrayList<Integer> customerIds = OrderPayment.getCustomerIdsForOrders(database, orders);
        boolean validation = customerIds.size()==1;
        if(!validation) {
            System.out.println("Assertion Error: Orders belong to multiple customers.");
        }
        return validation;
    }

    /**
     * Method to validate if the payment record exists for a given check number.
     * @param database the connection to the database.
     * @param cheque_number the check number.
     * @return true if the input is valid.
     * @throws SQLException
     */
    public static boolean validateChequeNumber(Connection database, String cheque_number) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_FIND_PAYMENT_BY_CHEQUE_NO);
        statement.setString(1, cheque_number);
        Integer count = QueryUtils.getResult(statement, Integer.class);
        if(count == null) {
            return false;
        } else if(count == 0) {
            System.out.println("Cheque Not Found");
            return false;
        } else if(count == 1) {
            return true;
        }  else if(count > 1) {
            System.out.println("Multiple entries of cheque found.");
            return false;
        }
        return false;
    }

    /**
     * Method to validate if the orderNumbers exists in the database.
     * @param database the connection to the database.
     * @param orders the list of order numbers.
     * @return true if the input is valid.
     * @throws SQLException
     */
    public static boolean validateOrderNumbers(Connection database, ArrayList<Integer> orders) throws SQLException {
        String inClause = QueryUtils.getInClause(orders.size());
        Set<Integer> set = new HashSet<>(orders);
        if(set.size()!=orders.size()) {
            orders.clear();
            orders.addAll(set);
        }
        String SQL = String.format(SQL_FIND_ORDER_BY_ORDER_NUMBER, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        QueryUtils.setInClauseParams(statement, orders, 1);
        Integer count = QueryUtils.getResult(statement, Integer.class);
        if(count == null) {
            return false;
        } else if (count == orders.size()) {
            return true;
        } else  {
            System.out.println("Few Orders were not found in the database");
            return false;
        }
    }

    /**
     * Method to validate if the given amount and orders add up to same values.
     * @param database the connection to the database.
     * @param amount the given amount
     * @param orders the list of order numbers.
     * @return true if the input is valid.
     * @throws SQLException
     */
    public static boolean validateOrderAmount(Connection database, float amount, ArrayList<Integer> orders) throws SQLException {
        String inClause = QueryUtils.getInClause(orders.size());
        String SQL = String.format(SQL_SUM_ORDERS_AMOUNT, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        QueryUtils.setInClauseParams(statement, orders, 1);
        Double val = QueryUtils.getResult(statement, Double.class);
        if(val == null) {
            return false;
        } else if (val.floatValue() == amount) {
            return true;
        }else {
            System.out.printf("Amount and Order values don't match for Order Ids: %s, Amount: %s \n", orders, amount);
            return false;
        }
    }

    /**
     * Method to validate if the check amount and given amount are equal.
     * @param database the connection to the database.
     * @param amount the given amount
     * @param cheque_number the check number.
     * @param orders the list of order numbers.
     * @return true if input is valid
     * @throws SQLException
     */
    public static boolean validateChequeAmount(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_GET_CHEQUE_AMOUNT);
        statement.setString(1, cheque_number);
        Double val = QueryUtils.getResult(statement, Double.class);
        if(val == null) {
            return false;
        } else if (val.floatValue() == amount) {
            return true;
        }else {
            System.out.println("Amount and Cheque values don't match.");
            return false;
        }
    }
}
