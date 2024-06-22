package visitor;

import java.util.ArrayList;
import java.util.List;

public class Client {
    
	public static void main(String[] args) {
		for(Employee emp : mockEmployee()) {
			emp.accept(new ConcreteVisitor());
		}
	}
	
	private static List<Employee> mockEmployee() {
		List<Employee> empList = new ArrayList<Employee>();
		//产生员工
		CommonEmployee tom = new CommonEmployee("Tom", 5000, Employee.MALE);
		tom.setJob("编写Java程序");
		empList.add(tom);
		
		CommonEmployee halen = new CommonEmployee("Halen", 4000, Employee.FEMALE);
		halen.setJob("页面美工");
		empList.add(halen);
		
		Manager jack = new Manager("Jack", 6700, Employee.MALE);
		jack.setPerformance("管理开发人员");
		empList.add(jack);
		
		return empList;
	}
}
