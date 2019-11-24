import java.sql.Connection;
import java.sql.Statement;

public class DbMigrationUtils {

    private static final String CREATE_TEMP_TABLE = "CREATE TABLE payments_temp (\n" +
            "\tpayment_id int NOT NULL auto_increment,\n" +
            "    customerNumber int NOT NULL,\n" +
            "    checkNumber varchar(50) NOT NULL,\n" +
            "    paymentDate date NOT NULL,\n" +
            "    amount decimal(10,2) NOT NULL,\n" +
            "    PRIMARY KEY (payment_id),\n" +
            "    CONSTRAINT payments_CN_FK FOREIGN KEY (customerNumber) REFERENCES customers (customerNumber)\n" +
            ")";

    private static final String COPY_TO_TEMP_TABLE = "" +
            "insert into payments_temp(customerNumber, checkNumber, paymentDate, amount) \n" +
            "select * from payments;";

    private static final String DROP_PAYMENTS_TABLE = "drop table payments;";

    private static final String RENAME_PAYMENTS_TEMP_TO_PAYMENTS_TABLE = "rename table payments_temp to payments;";

    private static final String CREATE_ORDER_PAYMENTS = "create table order_payment (\n" +
            "\tid int auto_increment primary key,\n" +
            "    orderNumber int,\n" +
            "    payment_id int,\n" +
            "    CONSTRAINT PAY_ORD foreign key (payment_id)\n" +
            "    REFERENCES payments (payment_id),\n" +
            "    UNIQUE(orderNumber, payment_id)" +
            ");";

    static public void addPaymentStatusColumn(Connection database) {
        String ADD_PAYMENT_STATUS = "" +
                "alter table orders\n" +
                "add column payment_status ENUM('PAID', 'UNPAID', 'PENDING', 'UNAVAILABLE') default 'UNAVAILABLE'";
        try {
            Statement stmt = database.createStatement();
            stmt.executeUpdate(ADD_PAYMENT_STATUS);
            System.out.println("[SUCCESS] Add PAYMENT_STATUS column to orders");
        } catch (Exception ignored) {
            System.out.println("[FAILED] Add PAYMENT_STATUS column to orders");
        }
    }

    static public void createSurrogateKeyInPayments(Connection database) {
        try {
            Statement stmt = database.createStatement();
            stmt.executeUpdate(CREATE_TEMP_TABLE);
            stmt.executeUpdate(COPY_TO_TEMP_TABLE);
            stmt.executeUpdate(DROP_PAYMENTS_TABLE);
            stmt.executeUpdate(RENAME_PAYMENTS_TEMP_TO_PAYMENTS_TABLE);
            System.out.println("[SUCCESS] Create PAYMENT_ID in Payments");
        } catch (Exception ignored) {
            System.out.println("[FAILED] Create PAYMENT_ID in Payments");
        }
    }

    static public void createOrderPaymentsTable(Connection database) {
        try {
            Statement stmt = database.createStatement();
            stmt.executeUpdate(CREATE_ORDER_PAYMENTS);
            System.out.println("[SUCCESS] Create ORDER_PAYMENTS table");
        } catch (Exception ignored) {
            System.out.println("[FAILED] Create ORDER_PAYMENTS table");
        }
    }

}
