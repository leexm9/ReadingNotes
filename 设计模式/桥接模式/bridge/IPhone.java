package bridge;

public class IPhone extends Product {

    @Override
    public void beProducted() {
        System.out.println("代工生产苹果手机...");
    }

    @Override
    public void beSelled() {
        System.out.println("生产的苹果手机被卖出了...");
    }

}
