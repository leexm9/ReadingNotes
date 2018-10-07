package chain;

public abstract class Handler {
	public final static int LOW_LEVEL = 1;
	public final static int MID_LEVEL = 2;
	public final static int TOP_LEVEL = 3;
	
	//能处理的级别
	private int level = 0;
			
	//责任传递，下一个责任人
    private Handler nextHandler;

    public Handler(int level) {
    	this.level = level;
    }
    
    public final void handMessage(Request request) {
    	if (request.getLevel() == this.level) {
    		this.response(request);
    	} else {
    		if (this.nextHandler != null) {
    			this.nextHandler.handMessage(request);
    		} else {
    			System.out.println("-----没有请示的地方了，按不同意处理-----");
    		}
    	}
    }
    
    /**
     * 处理聚餐费用的申请
     * @param request
     * @return
     */
    protected abstract void response(Request request);

	public Handler getNextHandler() {
		return nextHandler;
	}

	public void setNextHandler(Handler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
