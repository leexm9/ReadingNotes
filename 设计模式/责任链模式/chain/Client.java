package chain;

public class Client {

	public static void main(String[] args) {
		//建立责任链
		Handler project = new ProjectManager();
		Handler dept = new DeptManager();
		Handler boss = new Boss();
		project.setNextHandler(dept);
		dept.setNextHandler(boss);
		
		//请求
		System.out.println("----处理1号申请----");
		Request req1 = new Request();
		req1.setName("Tom");
		req1.setMoney(450);
		project.handMessage(req1);
		System.out.println();
		
		System.out.println("----处理2号申请----");
		Request req2 = new Request();
		req2.setName("Jack");
		req2.setMoney(860);
		project.handMessage(req2);
		System.out.println();
		
		System.out.println("----处理3号申请----");
		Request req3 = new Request();
		req3.setName("Helen");
		req3.setMoney(1860);
		project.handMessage(req3);
		System.out.println();
		
		System.out.println("----处理4号申请----");
		Request req4 = new Request();
		req4.setName("Jim");
		req4.setMoney(2860);
		project.handMessage(req4);
		System.out.println();
	}

}
