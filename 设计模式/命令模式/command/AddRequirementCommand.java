package command;

public class AddRequirementCommand extends Command {

	//执行增加需求命令
	@Override
	public void execute() {
		//找到需求租
		super.rg.find();
		//增加一项需求
		super.rg.add();
		//给出计划
		super.rg.plan();
	}

}
