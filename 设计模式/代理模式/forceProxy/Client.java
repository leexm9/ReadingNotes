package forceProxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //ֱ�ӷ�����ʵ��ɫ
        IGamePlayer player = new GamePlayer("����");
        //��ʼʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
        player.login("zhangsan", "1234");
        //����ʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
        
        System.out.println("------------------");
        //ֱ�ӷ��ʴ����ɫ
        IGamePlayer proxy = new GamePlayerProxy(player);
        System.out.println("��ʼʱ�䣺" + new Date());
        proxy.login("zhangsan", "1234");
        System.out.println("��ʼʱ�䣺" + new Date());
        
        System.out.println("------------------");
        //���ָ���Ĵ���
        proxy = player.getProxy();
        System.out.println("��ʼʱ�䣺" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        System.out.println("��ʼʱ�䣺" + new Date());
    }
}
