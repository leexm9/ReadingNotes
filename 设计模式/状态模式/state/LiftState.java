package state;

public abstract class LiftState {
    //定义一个环境角色，也就是封装状态变化引起的功能变化
    protected Context context;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    
    //首先电梯门开启动作
    public abstract void open();
    //关闭动作
    public abstract void close();
    //运行动作
    public abstract void run();
    //停止动作
    public abstract void stop();
}
