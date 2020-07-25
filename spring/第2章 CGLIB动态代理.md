## 第 2 章 CGLIB 动态代理

### 2.1 CGLIB 介绍

CGLIB(Code Generation Library)是一个开源项目！是一个强大的，高性能，高质量的Code生成类库，它可以在运行期扩展Java类与实现Java接口。Hibernate用它来实现PO(Persistent Object 持久化对象)字节码的动态生成。CGLIB是一个强大的高性能的代码生成包。它广泛的被许多AOP的框架使用，例如Spring AOP为他们提供方法的interception（拦截）。CGLIB包的底层是通过使用一个小而快的字节码处理框架ASM，来转换字节码并生成新的类。除了CGLIB包，脚本语言例如Groovy和BeanShell，也是使用ASM来生成java的字节码。当然不鼓励直接使用ASM，因为它要求你必须对JVM内部结构包括class文件的格式和指令集都很熟悉。

### 2.2 CGLIB 动态代理使用

```java
public class HelloService {

    public HelloService() {
        System.out.println("HelloService构造");
    }

    /**
     * 该方法不能被子类覆盖, Cglib是无法代理final修饰的方法的
     */
    final public String sayOthers(String name) {
        System.out.println("HelloService:sayOthers>>"+name);
        return null;
    }

    public void sayHello() {
        System.out.println("HelloService:sayHello");
    }

}

public class MyMethodInterceptor implements MethodInterceptor {

    /**
     * o：cglib生成的代理对象
     * method：被代理对象方法
     * objects：方法入参
     * methodProxy: 代理方法
     */
    /**
     *
     * @param o             cglib生成的代理对象
     * @param method        被代理对象方法
     * @param objects       方法入参
     * @param methodProxy   代理方法
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("======插入前置通知======");
        Object object = methodProxy.invokeSuper(o, objects);
        System.out.println("======插入后者通知======");
        return object;
    }

}

public class CglibTest {

    public static void main(String[] args) {
        // 代理类class文件存入本地磁盘方便我们反编译查看源码
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\code");
        // 通过CGLIB动态代理获取代理对象的过程
        Enhancer enhancer = new Enhancer();
        // 设置enhancer对象的父类
        enhancer.setSuperclass(HelloService.class);
        // 设置enhancer的回调对象
        enhancer.setCallback(new MyMethodInterceptor());
        // 创建代理对象
        HelloService proxy= (HelloService) enhancer.create();
        // 通过代理对象调用目标方法
        proxy.sayHello();
    }

}
```

输出：

```java
HelloService构造
======插入前置通知======
HelloService:sayHello
======插入后者通知======
```

### 2.3 CGLIB动态代理源码分析

实现CGLIB动态代理必须实现MethodInterceptor(方法拦截器)接口，源码如下:

```java
public interface MethodInterceptor extends Callback {
    /**
     * All generated proxied methods call this method instead of the original method.
     * The original method may either be invoked by normal reflection using the Method object,
     * or by using the MethodProxy (faster).
     * @param obj "this", the enhanced object
     * @param method intercepted Method
     * @param args argument array; primitive types are wrapped
     * @param proxy used to invoke super (non-intercepted method); may be called
     * as many times as needed
     * @throws Throwable any exception may be thrown; if so, super method will not be invoked
     * @return any value compatible with the signature of the proxied method. Method returning void will ignore this value.
     * @see MethodProxy
     */    
    public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, 
                            MethodProxy proxy) throws Throwable;
 
}
```

这个接口只有一个intercept()方法，这个方法有4个参数：

- obj 表示增强的对象，即实现这个接口类的一个对象
- method 表示要被拦截的方法
- args 表示要被拦截方法的参数
- proxy 表示要触发父类的方法对象

在上面的Client代码中，通过Enhancer.create()方法创建代理对象，create()方法的源码：

```java
/**
 * Generate a new class if necessary and uses the specified
 * callbacks (if any) to create a new object instance.
 * Uses the no-arg constructor of the superclass.
 * @return a new instance
 */
public Object create() {
  	classOnly = false;
  	argumentTypes = null;
  	return createHelper();
}
```

该方法含义就是如果有必要就创建一个新类，并且用指定的回调对象创建一个新的对象实例，使用的父类的参数的构造方法来实例化父类的部分。核心内容在createHelper()中，源码如下:

```java
private Object createHelper() {
            preValidate();
    Object key = KEY_FACTORY.newInstance((superclass != null) ? superclass.getName() : null,
            ReflectUtils.getNames(interfaces),
            filter == ALL_ZERO ? null : new WeakCacheKey<CallbackFilter>(filter),
            callbackTypes,
            useFactory,
            interceptDuringConstruction,
            serialVersionUID);
    this.currentKey = key;
    Object result = super.create(key);
    return result;
}
```

preValidate()方法校验callbackTypes、filter是否为空，以及为空时的处理。通过newInstance()方法创建EnhancerKey对象，作为Enhancer父类AbstractClassGenerator.create()方法创建代理对象的参数。

```java
protected Object create(Object key) {
    try {
        ClassLoader loader = getClassLoader();
        Map<ClassLoader, ClassLoaderData> cache = CACHE;
        ClassLoaderData data = cache.get(loader);
        if (data == null) {
            synchronized (AbstractClassGenerator.class) {
                cache = CACHE;
                data = cache.get(loader);
                if (data == null) {
                    Map<ClassLoader, ClassLoaderData> newCache = new WeakHashMap<ClassLoader, ClassLoaderData>(cache);
                    data = new ClassLoaderData(loader);
                    newCache.put(loader, data);
                    CACHE = newCache;
                }
            }
        }
        this.key = key;
        Object obj = data.get(this, getUseCache());
        if (obj instanceof Class) {
            return firstInstance((Class) obj);
        }
        return nextInstance(obj);
    } catch (RuntimeException e) {
        throw e;
    } catch (Error e) {
        throw e;
    } catch (Exception e) {
        throw new CodeGenerationException(e);
    }
}
```

真正创建代理对象方法在nextInstance()方法中，该方法为抽象类AbstractClassGenerator的一个方法

```java
protected Object nextInstance(Object instance) {
    EnhancerFactoryData data = (EnhancerFactoryData) instance;

    if (classOnly) {
        return data.generatedClass;
    }

    Class[] argumentTypes = this.argumentTypes;
    Object[] arguments = this.arguments;
    if (argumentTypes == null) {
        argumentTypes = Constants.EMPTY_CLASS_ARRAY;
        arguments = null;
    }
    return data.newInstance(argumentTypes, arguments, callbacks);
}
```

看看data.newInstance(argumentTypes, arguments, callbacks)方法，第一个参数为代理对象的构成器类型，第二个为代理对象构造方法参数，第三个为对应回调对象。最后根据这些参数，通过反射生成代理对象，源码如下:

```java
/**
 * Creates proxy instance for given argument types, and assigns the callbacks.
 * Ideally, for each proxy class, just one set of argument types should be used,
 * otherwise it would have to spend time on constructor lookup.
 * Technically, it is a re-implementation of {@link Enhancer#createUsingReflection(Class)},
 * with "cache {@link #setThreadCallbacks} and {@link #primaryConstructor}"
 *
 * @see #createUsingReflection(Class)
 * @param argumentTypes constructor argument types
 * @param arguments constructor arguments
 * @param callbacks callbacks to set for the new instance
 * @return newly created proxy
 */
public Object newInstance(Class[] argumentTypes, Object[] arguments, Callback[] callbacks) {
    setThreadCallbacks(callbacks);
    try {
        // Explicit reference equality is added here just in case Arrays.equals does not have one
        if (primaryConstructorArgTypes == argumentTypes ||
                Arrays.equals(primaryConstructorArgTypes, argumentTypes)) {
            // If we have relevant Constructor instance at hand, just call it
            // This skips "get constructors" machinery
            return ReflectUtils.newInstance(primaryConstructor, arguments);
        }
        // Take a slow path if observing unexpected argument types
        return ReflectUtils.newInstance(generatedClass, argumentTypes, arguments);
    } finally {
        // clear thread callbacks to allow them to be gc'd
        setThreadCallbacks(null);
    }

}
```

从生成的 class 反编译源码，可以看到

```java
public final void sayHello() {
  	MethodInterceptor var10000 = this.CGLIB$CALLBACK_0;
    if (var10000 == null) {
      	CGLIB$BIND_CALLBACKS(this);
      	var10000 = this.CGLIB$CALLBACK_0;
    }

    if (var10000 != null) {
     		var10000.intercept(this, CGLIB$sayHello$0$Method, CGLIB$emptyArgs, CGLIB$sayHello$0$Proxy);
    } else {
      	super.sayHello();
    }
}
```

从代理对象反编译源码可以知道，代理对象继承于HelloService，拦截器调用intercept()方法，intercept()方法由自定义MyMethodInterceptor实现，所以，最后调用MyMethodInterceptor中的intercept()方法，从而完成了由代理对象访问到目标对象的动态代理实现。
