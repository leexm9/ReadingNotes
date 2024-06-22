package mediator;

import java.util.Random;

public class Sale extends AbstractColleague {

	public Sale(AbstractMediator mediator) {
		super(mediator);
	}
	
	public void sellComputer(int num) {
    	super.mediator.execute("sale.sell", num);
        System.out.println("销售电脑：" + num + "台");
    }
	
	//反馈销售情况，0~100之间变化，0没人买，100极其畅销
    public int getSaleStatus() {
    	Random rand = new Random(System.currentTimeMillis());
        int status = rand.nextInt(100);
        System.out.println("电脑销售情况：" + status);
        return status;
    }
    
    //打折销售
    public void offSale() {
    	super.mediator.execute("sale.offSell");
    }
}
