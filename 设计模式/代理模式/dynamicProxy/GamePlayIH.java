package dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class GamePlayIH implements InvocationHandler {
    
    //�������ʵ��
    private Object object;
    
    public GamePlayIH(Object object) {
        this.setObject(object);
    }
    
    //���ñ�����ķ���
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(this.object, args);
        //�ڲ�Ӱ�챻�����෽��������£���������Զ��������߼�
        if(method.getName().equalsIgnoreCase("login")) {
            System.out.println("�ҵ��˺ű���¼��....");
        }
        return result;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

}
