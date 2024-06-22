package bridge;

public class HouseCorp extends Corp {

    public HouseCorp(Product product) {
        super(product);
    }

    @Override
    public void makeMoney() {
        super.makeMoney();
        System.out.println("房地厂公司赚钱了...");
    }

}
