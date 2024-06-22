package forceProxy;

public class GamePlayer implements IGamePlayer {
    
    private String name;
    
    //指定代理类
    private IGamePlayer proxy;
    
    /**
     * 构造函数限制创建者，并同时传递名字
     * @param gamePlayer
     * @param name
     */
    public GamePlayer(String name) {
        this.name = name;
    }
    
    @Override
    public IGamePlayer getProxy() {
        this.proxy = new GamePlayerProxy(this);
        return this.proxy;
    }
    
    @Override
    public void login(String user, String password) {
        if(this.isProxy()) {
            System.out.println("登录名为" + user + "的用户——" + this.name + " 登录成功！");
        } else {
            System.out.println("GamePlayer.killBoss()请使用指定的代理访问");
        }
    }

    @Override
    public void killBoss() {
        if(this.isProxy()) {
            System.out.println(this.name + " 在打怪");
        } else {
            System.out.println("GamePlayer.killBoss()请使用指定的代理访问");
        }
        
    }

    @Override
    public void upgrade() {
        if(this.isProxy()) {
            System.out.println(this.name + " 又生了一级");
        } else {
            System.out.println("GamePlayer.killBoss()请使用指定的代理访问");
        }
    }

    //校验是否是代理访问
    private boolean isProxy() {
        if(this.proxy == null) {
            return false;
        } else {
            return true;
        }
    }
}
