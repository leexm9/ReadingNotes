package template;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) throws IOException {
        System.out.println("----宝马-----");
        System.out.println("宝马是否需要喇叭？0-不需要 1-需要");
        String type = (new BufferedReader(new InputStreamReader(System.in))).readLine();
        BWM bwm = new BWM();
        if(type.equals("0")) {
            bwm.setAlarm(false);
        }
        bwm.run();
        
        System.out.println("----奥迪-----");
        Audi audi = new Audi();
        audi.run();
    }

}
