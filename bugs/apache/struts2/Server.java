

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.DefaultLocaleProvider;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.filter.StrutsPrepareFilter;
import org.apache.struts2.dispatcher.multipart.JakartaMultiPartRequest;
import org.apache.struts2.dispatcher.multipart.MultiPartRequestWrapper;
import org.apache.struts2.interceptor.FileUploadInterceptor;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Server {

    private FileUploadInterceptor interceptor;

    private File tempDir;

    protected ConfigurationManager configurationManager;

    protected Configuration configuration;

    private Container container;

    Server() {
        interceptor = new FileUploadInterceptor();
        configurationManager = new ConfigurationManager(Container.DEFAULT_NAME);
        configurationManager.addContainerProvider(new XWorkConfigurationProvider());
        configuration = configurationManager.getConfiguration();
        container = configuration.getContainer();
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server();
        server.test();
    }

    public void test() throws Exception {

        Request req = new Request(null, null);

        req.setContentType(getInput()); // not a multipart contentype
        req.setMethod("post");

        MyFileupAction action = new MyFileupAction();
        MockActionInvocation mai = new MockActionInvocation();
        ActionContext.setContext(new ActionContext(new HashMap<String,Object>()));

        mai.setAction(action);
        mai.setResultCode("success");
        mai.setInvocationContext(ActionContext.getContext());

        ActionContext.getContext().setParameters(HttpParameters.create().build());
        ActionContext.getContext().put(ActionContext.CONTAINER, container);
        ActionContext.getContext().put(ServletActionContext.HTTP_REQUEST, createMultipartRequestMaxSize(req, 2000));

        interceptor.intercept(mai);
    }
    private MultiPartRequestWrapper createMultipartRequestMaxSize(Request req, int maxsize) throws IOException {
        return createMultipartRequest(req, maxsize, -1, -1, -1);
    }

    private String getInput() {
        char[] chs = "multipart/form-data".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return 'm';
    }

    private MultiPartRequestWrapper createMultipartRequest(Request req, int maxsize, int maxfilesize,
                                                           int maxfiles, int maxStringLength) throws IOException {

        JakartaMultiPartRequest jak = new JakartaMultiPartRequest();
        jak.setMaxSize(String.valueOf(maxsize));
        //jak.setMaxFileSize(String.valueOf(maxfilesize));
        //jak.setMaxFiles(String.valueOf(maxfiles));
        //jak.setMaxStringLength(String.valueOf(maxStringLength));
        return new MultiPartRequestWrapper(jak, req, tempDir.getAbsolutePath(), new DefaultLocaleProvider());
    }

    public static class MyFileupAction extends ActionSupport {

        private static final long serialVersionUID = 6255238895447968889L;

        // no methods
    }

}
