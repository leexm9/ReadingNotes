package mediator;

public class Mediator extends AbstractMediator {

	@Override
	public void execute(String str, Object... objects) {
		if (str.equals("purchase.buy")) {
			this.buyComputer((Integer)objects[0]);
		} else if (str.equals("sale.sell")) {
			this.sellComputer((Integer)objects[0]);
		} else if (str.equals("sale.offSell")) {
			this.offSell();
		} else if (str.equals("stock.clear")) {
			this.clearStock();
		}
	}
	
	//采购电脑
	private void buyComputer(int num) {
		int saleStatus = super.sale.getSaleStatus();
    	if (saleStatus > 80) {	//销售良好
        	System.out.println("采购电脑：" + num + "台");
    		super.stock.increase(num);
        } else {
        	int buyNum = num / 2;  //折半采购
            System.out.println("采购电脑：" + buyNum + "台");
        }
	}

	//销售电脑
	private void sellComputer(int num) {
		if (super.stock.getStockNumber() < num) {		//库存不足
        	super.purchase.buyComputer(num);
        }
        System.out.println("销售电脑：" + num + "台");
        super.stock.decrease(num);
	}
	
	//折价销售电脑
	private void offSell(){
		System.out.println("打折销售电脑：" + stock.getStockNumber() + "台");
	}
	
	//清仓处理
	private void clearStock() {
		super.sale.offSale();
		super.purchase.refuseBuy();
	}
}
