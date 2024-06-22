package flyweight;

import java.util.HashMap;
import java.util.Map;

public class FlavorFactory {
	
	private Map<String, Coffee> pool = new HashMap<String, Coffee>();
	
	private static final FlavorFactory flavorFactory = new FlavorFactory();

	private FlavorFactory() {
		super();
	}
	
	public static FlavorFactory getInstance() {
		return flavorFactory;
	}
	
	public Coffee getCoffee(String flavor) {
		Coffee order = null;
		if (pool.containsKey(flavor)) {
			order = pool.get(flavor);
		} else {
			order = new FlavorCoffee(flavor);
			pool.put(flavor, order);
		}
		return order;
	}
	
	public int getTotalFlavorsMade() {
		return pool.size();
	}
	
}
