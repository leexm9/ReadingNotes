package composite;

import java.util.ArrayList;
import java.util.List;

public class Branch extends Corp {
	//下属
	private List<Corp> sub = new ArrayList<Corp>();
	
	public Branch(String name, String position, int salary) {
		super(name, position, salary);
	}
	
	public void addSub(Corp corp) {
		this.sub.add(corp);
	}
	
	public void removeSub(Corp corp) {
		this.sub.remove(corp);
	}
	
	public List<Corp> getSub() {
		return this.sub;
	}
}
