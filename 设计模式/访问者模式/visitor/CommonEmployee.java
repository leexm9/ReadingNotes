package visitor;

public class CommonEmployee extends Employee {

	private String job;
	
	public CommonEmployee(String name, int salary, int sex) {
		super(name, salary, sex);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

}
