package builder;

public class BWM extends CarModel {

    @Override
    protected void start() {
        System.out.println("BWM.start()");
    }

    @Override
    protected void stop() {
        System.out.println("BWM.stop()");
    }

    @Override
    protected void alarm() {
        System.out.println("BWM.alarm()");
    }

    @Override
    protected void engineBoom() {
        System.out.println("BWM.engineBoom()");
    }

}
