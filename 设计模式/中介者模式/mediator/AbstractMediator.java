package mediator;

public abstract class AbstractMediator {
	//采购管理
	protected Purchase purchase;
	//销售管理
	protected Sale sale;
	//库存管理
	protected Stock stock;
	
	public AbstractMediator() {
		purchase = new Purchase(this);
		sale = new Sale(this);
		stock = new Stock(this);
	}
	
	//中介者最重要的方法叫做事件方法，处理多个对象之间的关系
	public abstract void execute(String str, Object...objects);
}
