package commonProxy;

public class GamePlayer implements IGamePlayer {

    private String name;

    /**
     * 构造函数限制创建者，并同时传递名字
     * @param gamePlayer
     * @param name
     */
    public GamePlayer(IGamePlayer gamePlayer, String name) throws NullPointerException {
        if(gamePlayer == null) {
            //确保代理类不能为空
            throw new NullPointerException();
        } else {
            this.name = name;
        }
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
