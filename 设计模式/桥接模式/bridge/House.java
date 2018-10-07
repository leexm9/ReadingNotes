package bridge;

public class House extends Product {

    @Override
    public void beProducted() {
        System.out.println("生产多栋楼房...");
    }

    @Override
    public void beSelled() {
        System.out.println("生产的楼房被卖出去了...");
    }

}
