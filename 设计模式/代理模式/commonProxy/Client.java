package commonProxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //�������
        IGamePlayer proxy = new GamePlayerProxy("����");
        //��ʼʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //����ʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
    }
}
