package proxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //��ʵ���
        IGamePlayer player = new GamePlayer("����");
        //�������
        IGamePlayer proxy = new GamePlayerProxy(player);
        //��ʼʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //����ʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
    }
}
