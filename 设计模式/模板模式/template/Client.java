package template;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) throws IOException {
        System.out.println("----����-----");
        System.out.println("�����Ƿ���Ҫ���ȣ�0-����Ҫ 1-��Ҫ");
        String type = (new BufferedReader(new InputStreamReader(System.in))).readLine();
        BWM bwm = new BWM();
        if(type.equals("0")) {
            bwm.setAlarm(false);
        }
        bwm.run();
        
        System.out.println("----�µ�-----");
        Audi audi = new Audi();
        audi.run();
    }

}
