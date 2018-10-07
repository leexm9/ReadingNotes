package mediator;

public class Stock extends AbstractColleague {
	//原始库存数量
	private static int COMPUTER_NUMBER = 100;
	
	public Stock(AbstractMediator mediator) {
		super(mediator);
	}
	
	//增加库存
	public void increase(int num) {
		COMPUTER_NUMBER += num;
		System.out.println("库存数量：" + COMPUTER_NUMBER + "台");
	}
	
	//降低库存
	public void decrease(int num) {
		COMPUTER_NUMBER -= num;
		System.out.println("库存数量：" + COMPUTER_NUMBER + "台");
	}
	
	//获取库存数量
    public int getStockNumber() {
    	return COMPUTER_NUMBER;
    }
    
    //库存压力大，通知采购不要再采购，销售降价销售
    public void clearStock() {
    	System.out.println("清理库存数量：" + COMPUTER_NUMBER + "台");
        super.mediator.execute("stock.clear");
    }
}
