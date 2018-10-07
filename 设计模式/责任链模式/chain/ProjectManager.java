package chain;

public class ProjectManager extends Handler {
	
	public ProjectManager() {
		super(Handler.LOW_LEVEL);
	}

	@Override
	protected void response(Request request) {
		System.out.println("----项目经理处理请求-----");
		System.out.println("费用申请——申请人：" + request.getName() + ", 申请金额：" + request.getMoney());
		System.out.println("项目经理答复:同意");
	}

}
