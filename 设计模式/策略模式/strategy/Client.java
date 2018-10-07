package strategy;

public class Client {

	public static void main(String[] args) {
		Context context;
		System.out.println("----刚到吴国拆第一个锦囊----");
		context = new Context(new BackDoor());
		context.operate();
		System.out.println();
		
		System.out.println("----刘备乐不思蜀了，拆第二个锦囊----");
		context = new Context(new GreenLight());
		context.operate();
		System.out.println();
		
		System.out.println("----孙权派出追兵----");
		context = new Context(new BlockEnemy());
		context.operate();
		System.out.println();
	}

}
