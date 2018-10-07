package mediator;

public abstract class AbstractColleague {
	//中介者
	protected AbstractMediator mediator;
	
	public AbstractColleague(AbstractMediator mediator) {
		this.mediator = mediator;
	}
}
