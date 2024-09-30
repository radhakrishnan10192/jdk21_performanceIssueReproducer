package loadtest;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;

public class Engine {
    public static void main(String[] args) {

        LoadTester.host = getProperty("host", "localhost", args);
        LoadTester.duration = Long.parseLong(getProperty("duration", "100", args));
        LoadTester.user = Integer.parseInt(getProperty("user", "1", args));

        System.out.println("Host : " + LoadTester.host);
        System.out.println("Duration : " + LoadTester.duration);
        System.out.println("User : " + LoadTester.user);

        GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
                .resourcesDirectory(IDEPathHelper.mavenResourcesDirectory.toString())
                .resultsDirectory(IDEPathHelper.resultsDirectory.toString())
                .binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString());

        Gatling.fromMap(props.build());
    }


    public static String getProperty(String key, String defaultValue, String[] args){
        for(String arg : args){
            if(arg.contains(key)){
                return arg.split("=")[1];
            }
        }
        return defaultValue;
    }
}
