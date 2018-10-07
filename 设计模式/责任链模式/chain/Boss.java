package chain;

public class Boss extends Handler {

	public Boss() {
		super(Handler.TOP_LEVEL);
	}

	@Override
	protected void response(Request request) {
		System.out.println("----总经理处理请求-----");
		System.out.println("费用申请——申请人：" + request.getName() + ", 申请金额：" + request.getMoney());
		System.out.println("总经理答复:同意");
	}

}
