package visitor;

public class Manager extends Employee {
	//业绩
	private String performance;
	
	public Manager(String name, int salary, int sex) {
		super(name, salary, sex);
	}
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
	public String getPerformance() {
		return performance;
	}
	public void setPerformance(String performance) {
		this.performance = performance;
	}

}
