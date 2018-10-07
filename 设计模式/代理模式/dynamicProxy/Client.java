package dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Date;

public class Client {

    public static void main(String[] args) {
        //����һ����Ϸ���
        IGamePlayer player = new GamePlayer("����");
        //����һ��handler
        InvocationHandler handler = new GamePlayIH(player);
        System.out.println("��ʼʱ�䣺" + new Date());
        //���class��ļ�����loader
        ClassLoader loader = player.getClass().getClassLoader();
        
        //��̬����һ��������
//        IGamePlayer proxy = (IGamePlayer) Proxy.newProxyInstance(loader, player.getClass().getInterfaces(), handler);
        IGamePlayer proxy = (IGamePlayer) Proxy.newProxyInstance(loader, new Class[]{IGamePlayer.class}, handler);
        
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //����ʱ��
        System.out.println("��ʼʱ�䣺" + new Date());
    }

}
