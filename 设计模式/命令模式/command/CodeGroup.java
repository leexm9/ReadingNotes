package command;

public class CodeGroup extends Group {

	//客户要求需求租和他们谈
	@Override
	public void find() {
		System.out.println("找到后端组...");
	}

	//增加需求
	@Override
	public void add() {
		System.out.println("客户要求增加一项功能...");
	}

	@Override
	public void delete() {
		System.out.println("客户要求删除一项功能...");
	}

	@Override
	public void change() {
		System.out.println("客户要求修改一项功能...");
	}

	@Override
	public void plan() {
		System.out.println("客户要求变更功能计划...");
	}

}
