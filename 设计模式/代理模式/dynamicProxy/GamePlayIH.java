package dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class GamePlayIH implements InvocationHandler {
    
    //被代理的实例
    private Object object;
    
    public GamePlayIH(Object object) {
        this.setObject(object);
    }
    
    //调用被代理的方法
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(this.object, args);
        //在不影响被代理类方法的情况下，代理类可以额外增加逻辑
        if(method.getName().equalsIgnoreCase("login")) {
            System.out.println("我的账号被登录了....");
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
