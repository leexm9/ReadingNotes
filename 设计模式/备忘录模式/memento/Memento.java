package memento;

public class Memento {
	
	private int vitality;
	
	private int aggressivity;
	
	private int defencivity;

	//构造函数
	public Memento(int vitality, int aggressivity, int defencivity) {
		super();
		this.vitality = vitality;
		this.aggressivity = aggressivity;
		this.defencivity = defencivity;
	}
	
	public int getVitality() {
		return vitality;
	}

	public void setVitality(int vitality) {
		this.vitality = vitality;
	}

	public int getAggressivity() {
		return aggressivity;
	}

	public void setAggressivity(int aggressivity) {
		this.aggressivity = aggressivity;
	}

	public int getDefencivity() {
		return defencivity;
	}

	public void setDefencivity(int defencivity) {
		this.defencivity = defencivity;
	}

}
