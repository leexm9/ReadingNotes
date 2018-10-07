package state;

public class StoppingState extends LiftState {

    @Override
    public void open() {
        super.context.setCurrent(Context.openningState);
        super.context.open();
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public void run() {
        super.context.setCurrent(Context.runningState);
        super.context.run();
    }

    @Override
    public void stop() {
        System.out.println("电梯停止了...");
    }

}
