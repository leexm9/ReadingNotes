package builder;

import java.util.ArrayList;

public abstract class CarModel {
    //���������������������ִ��˳��
    private ArrayList<String> sequence = new ArrayList<String>();
    
    //������
    protected abstract void start();
    
    //��ͣ��
    protected abstract void stop();
    
    //������
    protected abstract void alarm();
    
    //���������
    protected abstract void engineBoom();
    
    //�����ڲ������߼�
    public final void run() {
        //ѭ��һ�ߣ�˭��ǰ������ִ�и÷���
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
    
    //����˳�򼯺�
    public final void setSequence(ArrayList<String> sequence) {
        this.sequence = sequence;
    }
}
