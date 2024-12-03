import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.cloud.config.server.resource.ResourceController;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.cloud.config.server.resource.GenericResourceRepository;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class Server {

    public static void main(String[] args) throws IOException {
        GenericResourceRepository resourceRepository = new GenericResourceRepository(
                new NativeEnvironmentRepository(new StandardEnvironment(), new NativeEnvironmentProperties()));
        resourceRepository.setResourceLoader(new FileSystemResourceLoader());
        ResourceController controller = new ResourceController(resourceRepository, null);
        controller.retrieve("name", "profile", getInput(), getHttpRequest(), true);
    }

    private static String getInput() {
        char[] chs = "label".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return 'l';
    }

    private static HttpServletRequest getHttpRequest() {
        return new MockHttpServletRequest();
    }

}
