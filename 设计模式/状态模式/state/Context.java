package state;

public class Context {
    //定义出电梯的所有状态
    public final static LiftState openningState = new OpenningState();
    public final static LiftState closingState = new ClosingState();
    public final static LiftState runningState = new RunningState();
    public final static LiftState stoppingState = new StoppingState();
    
    //定义电梯的但前状态
    private LiftState current;

    public LiftState getCurrent() {
        return current;
    }

    public void setCurrent(LiftState current) {
        this.current = current;
        //把当前的环境通知到各个实现类中
        this.current.setContext(this);
    }
    
    public void open() {
        this.current.open();
    }
    
    public void close() {
        this.current.close();
    }
    
    public void run() {
        this.current.run();
    }
    
    public void stop() {
        this.current.stop();
    }
}
