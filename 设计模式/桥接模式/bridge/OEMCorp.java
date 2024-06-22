package bridge;

public class OEMCorp extends Corp {

    public OEMCorp(Product product) {
        super(product);
    }

    @Override
    public void makeMoney() {
        super.makeMoney();
        System.out.println("代工厂赚钱了...");
    }

}
