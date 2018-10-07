package strategy;

public class Context {

	private Strategy strategy;

	public Context(Strategy strategy) {
		this.strategy = strategy;
	}
	
	public void operate() {
		this.strategy.operate();
	}
	
	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}
	
}
