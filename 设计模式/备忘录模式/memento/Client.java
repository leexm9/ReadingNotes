package memento;

public class Client {

	public static void main(String[] args) {
		//定义出发起人
		PlayerOriginator player = new PlayerOriginator(100, 100, 100);
        System.out.print("英雄初始的各项指标：");
        player.showState();

        //定义出备忘录管理员
        Caretaker taker = new Caretaker();
        taker.setMemento(player.createMemento());

        player.setVitality(70);
        player.setAggressivity(60);
        player.setDefencivity(20);

        System.out.print("英雄在大战Boss之后的各项指标");
        player.showState();

        //恢复备忘录
        player.restoreMemento(taker.getMemento());
        System.out.print("英雄回血之后的各项指标：");
        player.showState();
	}

}
