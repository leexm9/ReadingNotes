package commonProxy;

public class GamePlayer implements IGamePlayer {

    private String name;

    /**
     * ���캯�����ƴ����ߣ���ͬʱ��������
     * @param gamePlayer
     * @param name
     */
    public GamePlayer(IGamePlayer gamePlayer, String name) throws NullPointerException {
        if(gamePlayer == null) {
            //ȷ�������಻��Ϊ��
            throw new NullPointerException();
        } else {
            this.name = name;
        }
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
