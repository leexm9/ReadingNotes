package command;

public class Client {

	public static void main(String[] args) {
		Invoker invoker = new Invoker();	//调用者
		System.out.println("-----------客户增加需求-------------");
		//具体的命令
		Command command = new AddRequirementCommand();
		//调用者接收命令
		invoker.setCommand(command);
		//执行命令
		invoker.action();
		System.out.println("-----------客户需求删除一个页面-------------");
		//具体的命令
		command = new DeletePageCommand();
		//调用者接收命令
		invoker.setCommand(command);
		//执行命令
		invoker.action();
	}

}
