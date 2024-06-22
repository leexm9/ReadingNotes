package visitor;

public class ConcreteVisitor implements Visitor {
	//访问普通员工，打印出报表
	@Override
	public void visit(CommonEmployee commonEmployee) {
		System.out.println(this.getCommonEmployee(commonEmployee));
	}

	@Override
	public void visit(Manager manager) {
		System.out.println(this.getManagerInfo(manager));
	}

	private String getBasicInfo(Employee employee) {
		String info = "姓名：" + employee.getName() + "\t";
		info = info + "性别：" + (employee.getSex() == Employee.MALE ? "男" : "女") + "\t";
		info = info + "薪水：" + employee.getSalary() + "\t";
		return info;
	}
	
	private String getCommonEmployee(CommonEmployee commonEmployee) {
		String basicInfo = this.getBasicInfo(commonEmployee);
		String otherInfo = "工作：" + commonEmployee.getJob() + "\t";
		return basicInfo + otherInfo;
	}
	
	private String getManagerInfo(Manager manager) {
		String basicInfo = this.getBasicInfo(manager);
		String otherInfo = "业绩：" + manager.getPerformance() + "\t";
		return basicInfo + otherInfo;
	}
}
