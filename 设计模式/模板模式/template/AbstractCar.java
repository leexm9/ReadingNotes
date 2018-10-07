package template;

public abstract class AbstractCar {
    
       protected abstract void start();
       protected abstract void stop();
       protected abstract void alarm();
       protected abstract void engineBoom();
       
       //模板方法
       public final void run() {
           this.start();
           this.engineBoom();
           if(this.isAlarm()) {
               this.alarm();
           }
           this.stop();
       }
       
       //钩子方法，默认喇叭是响的
       protected boolean isAlarm() {
           return true;
       }
}
