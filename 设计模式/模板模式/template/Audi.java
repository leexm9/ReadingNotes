package template;

public class Audi extends AbstractCar {

    @Override
    protected void alarm() {
        System.out.println("奥迪鸣笛");
    }
    
    @Override
    protected void engineBoom() {
        System.out.println("奥迪发动机声音是这样的");
    }

    @Override
    protected void start() {
        System.out.println("奥迪发动");
    }
    
    @Override
    protected void stop() {
        System.out.println("奥迪停车");
    }
    
    //覆写钩子方法
    @Override
    protected boolean isAlarm() {
        return false;   //默认不需要喇叭
    }

}
