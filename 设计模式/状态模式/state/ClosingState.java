package state;

public class ClosingState extends LiftState {

    @Override
    public void open() {
        super.context.setCurrent(Context.openningState);
        super.context.open();
    }

    @Override
    public void close() {
        System.out.println("关闭电梯门...");
    }

    @Override
    public void run() {
        super.context.setCurrent(Context.runningState);
        super.context.run();
    }

    @Override
    public void stop() {
        super.context.setCurrent(Context.stoppingState);
        super.context.stop();
    }

}
