import org.postgresql.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class Server {
    public static void main(String[] args) throws SQLException {
        String jdbcUrl = getInput();
        DriverManager.registerDriver(new Driver());
        Connection connection = DriverManager.getConnection(jdbcUrl);
    }

    public static String getInput() {
        String loggerLevel = "debug";
        char[] chs = "./test.txt".toCharArray();
        chs[0] = taint();
        String loggerFile = new String(chs);
        String shellContent="test";
        return "jdbc:postgresql://127.0.0.1:5432/test/?loggerLevel="+loggerLevel+"&loggerFile="+loggerFile+ "&"+shellContent;
    }

    public static char taint(){
        return '.';
    }
}
