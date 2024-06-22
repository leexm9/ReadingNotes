package builder;

public class Audi extends CarModel {

    @Override
    protected void start() {
        System.out.println("Audi.start()");
    }

    @Override
    protected void stop() {
        System.out.println("Audi.stop()");
    }

    @Override
    protected void alarm() {
        System.out.println("Audi.alarm()");
    }

    @Override
    protected void engineBoom() {
        System.out.println("Audi.engineBoom()");
    }

}
