package chain;

public class Request {
	//申请人
	private String name;
	//申请的费用
	private int money;
	//申请的级别
	private int level;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getMoney() {
		return money;
	}
	public void setMoney(int money) {
		this.money = money;
		if (money < 500) {
			this.setLevel(1);
		} else if (money < 1000) {
			this.setLevel(2);
		} else if (money < 2000){
			this.setLevel(3);
		} else {
			this.setLevel(4);
		}
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
}
