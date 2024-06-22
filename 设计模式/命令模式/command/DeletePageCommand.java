package command;

public class DeletePageCommand extends Command {

	@Override
	public void execute() {
		//找到前端组
		super.pg.find();
		//删除一个页面
		super.rg.delete();
		//给出变更计划
		super.rg.plan();
	}

}
