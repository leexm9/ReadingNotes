package state;

public class RunningState extends LiftState {

    @Override
    public void open() {
        //do nothing
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public void run() {
        System.out.println("电梯正在运行...");
    }

    @Override
    public void stop() {
        super.context.setCurrent(Context.stoppingState);
        super.context.stop();
    }

}
