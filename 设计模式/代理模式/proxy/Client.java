package proxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //真实玩家
        IGamePlayer player = new GamePlayer("张三");
        //代练玩家
        IGamePlayer proxy = new GamePlayerProxy(player);
        //开始时间
        System.out.println("开始时间：" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //结束时间
        System.out.println("开始时间：" + new Date());
    }
}
