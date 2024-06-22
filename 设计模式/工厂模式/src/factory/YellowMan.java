package factory;

public class YellowMan extends Human {

    @Override
    public void getColor() {
        System.out.println("黄色人种黄皮肤");
    }

    @Override
    public void talk() {
        System.out.println("黄色人种说方言");
    }

}
