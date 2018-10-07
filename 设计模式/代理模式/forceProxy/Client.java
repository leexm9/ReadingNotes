package forceProxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //直接访问真实角色
        IGamePlayer player = new GamePlayer("张三");
        //开始时间
        System.out.println("开始时间：" + new Date());
        player.login("zhangsan", "1234");
        //结束时间
        System.out.println("开始时间：" + new Date());
        
        System.out.println("------------------");
        //直接访问代理角色
        IGamePlayer proxy = new GamePlayerProxy(player);
        System.out.println("开始时间：" + new Date());
        proxy.login("zhangsan", "1234");
        System.out.println("开始时间：" + new Date());
        
        System.out.println("------------------");
        //获得指定的代理
        proxy = player.getProxy();
        System.out.println("开始时间：" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        System.out.println("开始时间：" + new Date());
    }
}
