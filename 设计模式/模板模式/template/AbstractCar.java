package template;

public abstract class AbstractCar {
    
       protected abstract void start();
       protected abstract void stop();
       protected abstract void alarm();
       protected abstract void engineBoom();
       
       //ģ�巽��
       public final void run() {
           this.start();
           this.engineBoom();
           if(this.isAlarm()) {
               this.alarm();
           }
           this.stop();
       }
       
       //���ӷ�����Ĭ�����������
       protected boolean isAlarm() {
           return true;
       }
}
