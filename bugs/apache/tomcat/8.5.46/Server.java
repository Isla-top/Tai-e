import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.ApplicationFilterChain;
import org.apache.catalina.core.ApplicationFilterFactory;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

public class Server {
    public static void main(String[] args) throws ServletException, IOException {
        Request request = new Request();
        DefaultServlet defaultServlet = new DefaultServlet();
        defaultServlet.init();
        ApplicationFilterChain filterChain = ApplicationFilterFactory.createFilterChain(request, new StandardWrapper(),
                defaultServlet);
        Request req = new Request();
        req.setAttribute("javax.servlet.include.request_uri", "/");
        String souce = Server.getSource();
        req.setAttribute("javax.servlet.include.path_info", souce);
        req.setAttribute("javax.servlet.include.servlet_path", "/");
        filterChain.doFilter(req, new Response());
    }

    public static String getSource(){
        return new String(taintWarp());
    }

    public static char[] taintWarp(){
        char[] chs = new char[]{ 'W', 'E', 'B', '-', 'I', 'N', 'F', '/', 'T', 'e', 's', 't', '.', 't', 'x', 't' };
        chs[1] = taint();
        return chs;
    }

    public static char taint(){
        return 'E';
    }
}
