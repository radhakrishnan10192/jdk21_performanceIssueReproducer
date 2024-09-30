package loadtest;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.netty.util.internal.StringUtil;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class LoadTester extends Simulation {

    public static String host = System.getProperty("host");

    public static Boolean secure = Boolean.valueOf(System.getProperty("secure", "true"));

    public static String port = System.getProperty("port", "8443");
    public static long duration = StringUtil.isNullOrEmpty(System.getProperty("duration")) ? 10 : Integer.parseInt(System.getProperty("duration")) * 60;
    public static int user = StringUtil.isNullOrEmpty(System.getProperty("user")) ? 1 : Integer.parseInt(System.getProperty("user"));

    private String baseUrl = (secure ? "https://" : "http://") + host + ":" + port + "/echo?delay=10&size=100000&calls=5";

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .maxConnectionsPerHost(200)
            .shareConnections();

    private PopulationBuilder scenario2 = scenario("5-10-100000-Async")
            .exec(http("5-10-100000-Async").get("").check(status().is(200)))
            .injectClosed(constantConcurrentUsers(user).during(duration))
            .protocols(httpProtocol);
    {
        setUp(scenario2);
    }

    @Override
    public void before() {
        System.out.println("Base url : " + baseUrl);
        System.out.println("Simulation running on " + host + " for " + duration + " secs with users " + user + " Post : " + port);
    }

    @Override
    public void after() {
        System.out.println("Simulation is finished!");

    }
}
