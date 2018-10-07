package mediator;

public class Client {
	public static void main(String[] args) {
		AbstractMediator mediator = new Mediator();
		//采购电脑
		System.out.println("----采购电脑----");
		Purchase purchase = new Purchase(mediator);
		purchase.buyComputer(100);
		
		//销售电脑
		System.out.println("----销售电脑----");
		Sale sale = new Sale(mediator);
		sale.sellComputer(50);
		
		//库存管理
		System.out.println("----清理库存----");
		Stock stock = new Stock(mediator);
		stock.clearStock();
	}
}
