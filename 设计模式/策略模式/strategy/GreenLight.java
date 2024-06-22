package strategy;

public class GreenLight implements Strategy {

	@Override
	public void operate() {
		System.out.println("找吴国太帮忙");
	}

}
