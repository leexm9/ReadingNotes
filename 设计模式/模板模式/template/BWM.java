package template;

public class BWM extends AbstractCar {

    private boolean alarmFlag = true; //要喇叭响
    @Override
    protected void alarm() {
        System.out.println("宝马鸣笛");
    }
    
    @Override
    protected void engineBoom() {
        System.out.println("宝马发动机声音是这样的");
    }

    @Override
    protected void start() {
        System.out.println("宝马发动");
    }
    
    @Override
    protected void stop() {
        System.out.println("宝马停车");
    }
    
    //覆写钩子方法
    @Override
    protected boolean isAlarm() {
        return this.alarmFlag;
    }
    
    //要不要喇叭，是由高层模块决定
    public void setAlarm(boolean isAlarm) {
        this.alarmFlag = isAlarm;
    }

}
