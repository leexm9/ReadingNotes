package command;

public class RequirementGroup extends Group {

	//客户要求需求租和他们谈
	@Override
	public void find() {
		System.out.println("找到需求租...");
	}

	//增加需求
	@Override
	public void add() {
		System.out.println("客户要求增加一项新的需求...");
	}

	@Override
	public void delete() {
		System.out.println("客户要求删除一项需求...");
	}

	@Override
	public void change() {
		System.out.println("客户要求修改一项需求...");
	}

	@Override
	public void plan() {
		System.out.println("客户要求变更计划...");
	}

}
