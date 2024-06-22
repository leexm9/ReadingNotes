package dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Date;

public class Client {

    public static void main(String[] args) {
        //定义一个游戏玩家
        IGamePlayer player = new GamePlayer("张三");
        //定义一个handler
        InvocationHandler handler = new GamePlayIH(player);
        System.out.println("开始时间：" + new Date());
        //获得class类的加载类loader
        ClassLoader loader = player.getClass().getClassLoader();
        
        //动态产生一个代理者
//        IGamePlayer proxy = (IGamePlayer) Proxy.newProxyInstance(loader, player.getClass().getInterfaces(), handler);
        IGamePlayer proxy = (IGamePlayer) Proxy.newProxyInstance(loader, new Class[]{IGamePlayer.class}, handler);
        
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //结束时间
        System.out.println("开始时间：" + new Date());
    }

}
