package state;

public class OpenningState extends LiftState {

    @Override
    public void open() {
        System.out.println("开启电梯门...");
    }

    @Override
    public void close() {
        //状态修改
        super.context.setCurrent(Context.closingState);
        //执行动作
        super.context.close();
    }

    @Override
    public void run() {
        //do nothing
    }

    @Override
    public void stop() {
        //do nothing
    }

}
