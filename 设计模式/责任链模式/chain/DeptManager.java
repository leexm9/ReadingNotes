package chain;

public class DeptManager extends Handler {

	public DeptManager() {
		super(Handler.MID_LEVEL);
	}

	@Override
	protected void response(Request request) {
		System.out.println("----部门经理处理请求-----");
		System.out.println("费用申请——申请人：" + request.getName() + ", 申请金额：" + request.getMoney());
		System.out.println("部门经理答复:同意");
	}

}
