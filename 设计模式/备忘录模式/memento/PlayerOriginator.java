package memento;

public class PlayerOriginator {
	private int vitality; //生命力
	private int aggressivity; //攻击力
	private int defencivity; //防御力
	
	public PlayerOriginator(int vitality, int aggressivity, int defencivity){
	    this.vitality = vitality;
	    this.aggressivity = aggressivity;
	    this.defencivity = defencivity;
	}
	
	//创建一个备忘录
	public Memento createMemento(){
	    Memento memento = new Memento(this.vitality, this.aggressivity, this.defencivity);
	    return memento;
	}
	
	//恢复一个备忘录
	public void restoreMemento(Memento memento){
	    this.vitality = memento.getVitality();
	    this.aggressivity = memento.getAggressivity();
	    this.defencivity = memento.getDefencivity();
	}
	
	public int getVitality(){
	    return vitality;
	}
	
	public void setVitality(int vitality){
	    this.vitality = vitality;
	}
	
	public int getAggressivity(){
	    return aggressivity;
	}
	
	public void setAggressivity(int aggressivity){
	    this.aggressivity = aggressivity;
	}
	
	public int getDefencivity(){
	    return defencivity;
	}
	
	public void setDefencivity(int defencivity){
	    this.defencivity = defencivity;
	}
	
	public void showState(){
	    System.out.print("vitality:" + this.vitality);
	    System.out.print("; aggressivity:" + this.aggressivity);
	    System.out.println("; defencivity:" + this.defencivity);
	}
}
