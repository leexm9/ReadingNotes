package visitor;

public interface Visitor {
	//访问普通员工
	public void visit(CommonEmployee commonEmployee);
	
	//访问经理
	public void visit(Manager manager);
}
