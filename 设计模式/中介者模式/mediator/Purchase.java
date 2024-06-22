package mediator;

public class Purchase extends AbstractColleague {

	public Purchase(AbstractMediator mediator) {
		super(mediator);
	}
	
	//采购电脑
	public void buyComputer(int num) {
		super.mediator.execute("purchase.buy", num);
	}
	
	//不再采购
	public void refuseBuy() {
		System.out.println("不再采购电脑");
	}
}
