package template;

public class BWM extends AbstractCar {

    private boolean alarmFlag = true; //Ҫ������
    @Override
    protected void alarm() {
        System.out.println("��������");
    }
    
    @Override
    protected void engineBoom() {
        System.out.println("��������������������");
    }

    @Override
    protected void start() {
        System.out.println("������");
    }
    
    @Override
    protected void stop() {
        System.out.println("����ͣ��");
    }
    
    //��д���ӷ���
    @Override
    protected boolean isAlarm() {
        return this.alarmFlag;
    }
    
    //Ҫ��Ҫ���ȣ����ɸ߲�ģ�����
    public void setAlarm(boolean isAlarm) {
        this.alarmFlag = isAlarm;
    }

}
