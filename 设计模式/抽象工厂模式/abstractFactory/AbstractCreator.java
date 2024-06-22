package abstractFactory;

public abstract class AbstractCreator {
    //创建A产品族
    public abstract AbstractProductA createProductA();
    
    //创建B产品族
    public abstract AbstractProductB createProductB();
}
