package command;

public class Invoker {
	//命令
	private Command command;

	//执行命令
	public void action() {
		this.command.execute();
	}
	
	public Command getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = command;
	}
	
}
