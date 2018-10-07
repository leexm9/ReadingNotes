package proxy;

public class GamePlayer implements IGamePlayer {
    
    //名字
    private String name;
    
    public GamePlayer(String name) {
        this.name = name;
    }
    
    @Override
    public void login(String user, String password) {
        System.out.println("登录名为" + user + "的用户——" + this.name + " 登录成功！");
    }

    @Override
    public void killBoss() {
        System.out.println(this.name + " 在打怪");
    }

    @Override
    public void upgrade() {
        System.out.println(this.name + " 又生了一级");
    }

}
