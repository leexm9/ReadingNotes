package template;

public class Audi extends AbstractCar {

    @Override
    protected void alarm() {
        System.out.println("�µ�����");
    }
    
    @Override
    protected void engineBoom() {
        System.out.println("�µϷ�����������������");
    }

    @Override
    protected void start() {
        System.out.println("�µϷ���");
    }
    
    @Override
    protected void stop() {
        System.out.println("�µ�ͣ��");
    }
    
    //��д���ӷ���
    @Override
    protected boolean isAlarm() {
        return false;   //Ĭ�ϲ���Ҫ����
    }

}
