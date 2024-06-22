package flyweight;

import java.util.ArrayList;
import java.util.List;

public class Client {
	public static void main(String[] args) {
	    // 订单对象生成工厂
		FlavorFactory flavorFactory = FlavorFactory.getInstance();
		// 客户下的订单
		List<Coffee> orders = new ArrayList<Coffee>();
		 // 增加订单
	    orders.add(flavorFactory.getCoffee("摩卡"));
	    orders.add(flavorFactory.getCoffee("卡布奇诺"));
	    orders.add(flavorFactory.getCoffee("香草星冰乐"));
	    orders.add(flavorFactory.getCoffee("香草星冰乐"));
	    orders.add(flavorFactory.getCoffee("拿铁"));
	    orders.add(flavorFactory.getCoffee("卡布奇诺"));
	    orders.add(flavorFactory.getCoffee("拿铁"));
	    orders.add(flavorFactory.getCoffee("卡布奇诺"));
	    orders.add(flavorFactory.getCoffee("摩卡"));
	    orders.add(flavorFactory.getCoffee("香草星冰乐"));
	    orders.add(flavorFactory.getCoffee("卡布奇诺"));
	    orders.add(flavorFactory.getCoffee("摩卡"));
	    orders.add(flavorFactory.getCoffee("香草星冰乐"));
	    orders.add(flavorFactory.getCoffee("拿铁"));
	    orders.add(flavorFactory.getCoffee("拿铁"));
	    // 卖咖啡
	    for (Coffee order : orders) {
	    	order.sell();
	    }
	    
	    // 打印生成的订单java对象数量
	    System.out.println("\n客户一共买了 " + orders.size() + " 杯咖啡!");
	    
	    // 打印生成的订单java对象数量
	    System.out.println("共生成了 " + flavorFactory.getTotalFlavorsMade() + " 个 FlavorCoffee java对象!");
	}
}
