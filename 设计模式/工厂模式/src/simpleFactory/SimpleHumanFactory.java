package simpleFactory;

import factory.Human;

public class SimpleHumanFactory {

    @SuppressWarnings("unchecked")
    public static <T extends Human> T createHuman(Class<T> c) {
        Human human = null;
        try {
            human = (T) Class.forName(c.getName()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) human;
    }
}
