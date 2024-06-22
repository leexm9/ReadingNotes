package visitor;

public abstract class Employee {
	public final static int MALE = 0; 	//男性
	public final static int FEMALE = 1;  //女性
	
	private int salary;
	private String name;
	private int sex;
	
	public Employee(String name, int salary, int sex) {
		super();
		this.salary = salary;
		this.name = name;
		this.sex = sex;
	}
	
	//允许访问者访问
	public abstract void accept(Visitor visitor);

	public int getSalary() {
		return salary;
	}

	public void setSalary(int salary) {
		this.salary = salary;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}
}
