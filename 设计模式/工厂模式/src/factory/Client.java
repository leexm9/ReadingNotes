package factory;

public class Client {
    public static void main(String[] args) {
        AbstractHumanFactory factory = new HumanFactory();
        Human black = factory.createHuman(BlackMan.class);
        Human white = factory.createHuman(WhiteMan.class);
        Human yellow = factory.createHuman(YellowMan.class);
        black.talk();
        white.talk();
        yellow.talk();
    }
}
