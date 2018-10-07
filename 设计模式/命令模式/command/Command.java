package command;

public abstract class Command {
	//需求组
	protected RequirementGroup rg = new RequirementGroup();
	//前端组
	protected PageGroup pg = new PageGroup();
	//后端组
	protected CodeGroup cg = new CodeGroup();
	
	//命令方法
	public abstract void execute();
}
