package command;

public class PageGroup extends Group {

	//客户要求需求租和他们谈
	@Override
	public void find() {
		System.out.println("找到前端组...");
	}

	//增加需求
	@Override
	public void add() {
		System.out.println("客户要求增加一个新的页面...");
	}

	@Override
	public void delete() {
		System.out.println("客户要求删除一个页面...");
	}

	@Override
	public void change() {
		System.out.println("客户要求修改一个页面...");
	}

	@Override
	public void plan() {
		System.out.println("客户要求变更页面计划...");
	}

}
