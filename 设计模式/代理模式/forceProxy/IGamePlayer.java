package forceProxy;

/**
 * ��Ϸ�߽��
 * @author leexm
 *
 */
public interface IGamePlayer {
    //��¼��Ϸ
    public void login(String user, String password);
    
    //��Ϸ��Ŀ�꣬ɱ������
    public void killBoss();
    
    //��Ϸ��ɫ����
    public void upgrade();
    
    public IGamePlayer getProxy();
}
