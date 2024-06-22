package commonProxy;

import java.util.Date;

public class Client {
    public static void main(String[] args) {
        //代练玩家
        IGamePlayer proxy = new GamePlayerProxy("张三");
        //开始时间
        System.out.println("开始时间：" + new Date());
        proxy.login("zhangsan", "1234");
        proxy.killBoss();
        proxy.upgrade();
        //结束时间
        System.out.println("开始时间：" + new Date());
    }
}
