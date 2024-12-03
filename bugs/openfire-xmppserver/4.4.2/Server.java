import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.MultiMap;
import org.jivesoftware.util.FaviconServlet;

import javax.servlet.ServletException;

public class Server {
    public static void main(String[] args) throws ServletException {
        FaviconServlet servlet = new FaviconServlet();
        servlet.init(null);
        Request req = new Request(null, null);

        MultiMap<String> mp = new MultiMap<>();
        mp.add("host", getTaintHost());
        req.setContentParameters(mp);
        String host = req.getParameter("host");
        servlet.doGet(req, new Response(null, null)); // goto sink
        // AVD-2019-18394 CVE-2019-18394
    }

    public static String getTaintHost(){
        char[] chs = new char[]{'1','9','2'};
        chs[1] = taint();
        return new String(chs);
    }

    public static char taint(){
        return '1';
    }

}
