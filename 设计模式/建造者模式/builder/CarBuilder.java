package builder;

import java.util.ArrayList;

public abstract class CarBuilder {
    
    //设置车的组装顺序
    public abstract void setSequence(ArrayList<String> sequence);
    
    //设置完顺序后，获得车的模型
    public abstract CarModel getCarModel();
}
