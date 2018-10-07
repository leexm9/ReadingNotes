package forceProxy;

/**
 * 游戏者借口
 * @author leexm
 *
 */
public interface IGamePlayer {
    //登录游戏
    public void login(String user, String password);
    
    //游戏的目标，杀掉大反派
    public void killBoss();
    
    //游戏角色升级
    public void upgrade();
    
    public IGamePlayer getProxy();
}
