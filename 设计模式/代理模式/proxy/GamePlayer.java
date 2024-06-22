package proxy;

public class GamePlayer implements IGamePlayer {
    
    //����
    private String name;
    
    public GamePlayer(String name) {
        this.name = name;
    }
    
    @Override
    public void login(String user, String password) {
        System.out.println("��¼��Ϊ" + user + "���û�����" + this.name + " ��¼�ɹ���");
    }

    @Override
    public void killBoss() {
        System.out.println(this.name + " �ڴ��");
    }

    @Override
    public void upgrade() {
        System.out.println(this.name + " ������һ��");
    }

}
