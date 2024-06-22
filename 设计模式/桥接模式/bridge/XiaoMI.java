package bridge;

public class XiaoMI extends Product {

    @Override
    public void beProducted() {
        System.out.println("代工生产小米手机...");
    }

    @Override
    public void beSelled() {
        System.out.println("生产的小米手机被卖出了...");
    }

}
