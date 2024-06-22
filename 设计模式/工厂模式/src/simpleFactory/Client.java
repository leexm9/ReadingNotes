package simpleFactory;

import factory.BlackMan;
import factory.Human;
import factory.WhiteMan;
import factory.YellowMan;

public class Client {
    public static void main(String[] args) {
        Human black = SimpleHumanFactory.createHuman(BlackMan.class);
        Human white = SimpleHumanFactory.createHuman(WhiteMan.class);
        Human yellow = SimpleHumanFactory.createHuman(YellowMan.class);
        black.talk();
        white.talk();
        yellow.talk();
    }
}
