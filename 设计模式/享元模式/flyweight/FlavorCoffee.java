package flyweight;

public class FlavorCoffee extends Coffee {
	//口味
	private String flavor;
	
	public FlavorCoffee(String flavor) {
		super();
		this.flavor = flavor;
	}

	@Override
	public void sell() {
		System.out.println("卖出一份" + flavor + "的咖啡。");
	}

}
