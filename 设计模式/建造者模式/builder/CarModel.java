package builder;

import java.util.ArrayList;

public abstract class CarModel {
    //这个参数决定各个方法的执行顺序
    private ArrayList<String> sequence = new ArrayList<String>();
    
    //车启动
    protected abstract void start();
    
    //车停下
    protected abstract void stop();
    
    //车鸣笛
    protected abstract void alarm();
    
    //车引擎轰鸣
    protected abstract void engineBoom();
    
    //车的内部运行逻辑
    public final void run() {
        //循环一边，谁在前，就先执行该方法
        for(String action : sequence) {
            if (action.equalsIgnoreCase("start")) {
                this.start();
            } else if (action.equalsIgnoreCase("stop")) {
                this.stop();
            } else if (action.equalsIgnoreCase("alarm")) {
                this.alarm();
            } else if (action.equalsIgnoreCase("engine boom")) {
                this.engineBoom();
            }
        }
    }
    
    //设置顺序集合
    public final void setSequence(ArrayList<String> sequence) {
        this.sequence = sequence;
    }
}
