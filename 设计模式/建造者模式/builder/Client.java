package builder;

public class Client {

    public static void main(String[] args) {
        Director director = new Director();
        System.out.println("--------BWM1------");
        director.getBWM1().run();
        System.out.println("--------BWM2------");
        director.getBWM2().run();
        System.out.println("--------Audi1------");
        director.getAudi1().run();
        System.out.println("--------Audi2------");
        director.getAudi2().run();
    }

}
