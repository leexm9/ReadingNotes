package builder;

import java.util.ArrayList;

public abstract class CarBuilder {
    
    //���ó�����װ˳��
    public abstract void setSequence(ArrayList<String> sequence);
    
    //������˳��󣬻�ó���ģ��
    public abstract CarModel getCarModel();
}
