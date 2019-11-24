import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValidationUtils {
    static final String SQL_FIND_PAYMENT_BY_CHEQUE_NO = "" +
            "select count(*) from payments\n" +
            "where checkNumber = ?;";

    static final String SQL_FIND_ORDER_BY_ORDER_NUMBER = "" +
            "select count(*) from orders\n" +
            "where orderNumber in (%s);";

    static final String SQL_SUM_ORDERS_AMOUNT = "" +
            "select sum(od.quantityOrdered * od.priceEach)\n" +
            "from orders as o\n" +
            "natural join orderdetails as od\n" +
            "where o.status = \"Shipped\"\n" +
            "and o.orderNumber in (%s)\n";
    static final String SQL_GET_CHEQUE_AMOUNT = "" +
            "select amount from payments\n" +
            "where checkNumber = ?;";

    static final String SQL_CHECK_CUSTOMER_VALIDATION = "" +
            "select orderNumber from orders\n" +
            "where orderNumber in (%s)\n" +
            "and customerNumber not in (select customerNumber from  payments where checkNumber = ?)";

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

    private static boolean validateSameCustomer(Connection database, String cheque_number, ArrayList<Integer> orders) throws SQLException {
        String inClause = getInClause(orders.size());
        String SQL = String.format(SQL_CHECK_CUSTOMER_VALIDATION, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        setInClauseParams(statement, orders, 1);
        statement.setString(orders.size()+1, cheque_number);
        ResultSet set = statement.executeQuery();
        if(set.next()) {
            System.out.println("Assertion Error: Order's Customer and Cheque's Customer dont match.");
            return false;
        }
        return false;
    }

    public static boolean validateChequeNumber(Connection database, String cheque_number) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_FIND_PAYMENT_BY_CHEQUE_NO);
        statement.setString(1, cheque_number);
        Integer count = getResult(statement, Integer.class);
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
    public static boolean validateOrderNumbers(Connection database, ArrayList<Integer> orders) throws SQLException {
        String inClause = getInClause(orders.size());
        Set<Integer> set = new HashSet<>(orders);
        if(set.size()!=orders.size()) {
            orders.clear();
            orders.addAll(set);
        }
        String SQL = String.format(SQL_FIND_ORDER_BY_ORDER_NUMBER, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        setInClauseParams(statement, orders, 1);
        Integer count = getResult(statement, Integer.class);
        if(count == null) {
            return false;
        } else if (count == orders.size()) {
            return true;
        } else  {
            System.out.println("Few Orders were not found in the database");
            return false;
        }
    }
    public static boolean validateOrderAmount(Connection database, float amount, ArrayList<Integer> orders) throws SQLException {
        String inClause = getInClause(orders.size());
        String SQL = String.format(SQL_SUM_ORDERS_AMOUNT, inClause);
        PreparedStatement statement = database.prepareStatement(SQL);
        setInClauseParams(statement, orders, 1);
        Double val = getResult(statement, Double.class);
        if(val == null) {
            return false;
        } else if (val.floatValue() == amount) {
            return true;
        }else {
            System.out.println("Amount and Order values don't match.");
            return false;
        }
    }
    public static boolean validateChequeAmount(Connection database, float amount, String cheque_number, ArrayList<Integer> orders) throws SQLException {
        PreparedStatement statement = database.prepareStatement(SQL_GET_CHEQUE_AMOUNT);
        statement.setString(1, cheque_number);
        Double val = getResult(statement, Double.class);
        if(val == null) {
            return false;
        } else if (val.floatValue() == amount) {
            return true;
        }else {
            System.out.println("Amount and Cheque values don't match.");
            return false;
        }
    }

    public static <T> T getResult(PreparedStatement statement, Class<T> tClass) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            return resultSet.getObject(1, tClass);
        }
        return null;
    }

    private static String getInClause(Integer length) {
        StringBuilder builder = new StringBuilder("");
        for (int i = 0; i < length; i++) {
            builder.append("?");
            if(i!=length-1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private static <T> void setInClauseParams(PreparedStatement statement, List<T> list, Integer startAt) throws SQLException {
        for (T obj :
                list) {
            statement.setObject(startAt, obj);
            startAt = startAt + 1;
        }
    }
}
