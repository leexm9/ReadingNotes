## 第 16 章 Class.forName、ClassLoader 以及 spi

[toc]

### 16.1 Class.forName() 与 ClassLoader

Class.forName() 和 ClassLoader 都可以对类进行加载。ClassLoader 遵循双亲委派模型，实现的功能是“通过一个类的全限定名来获取描述此类的二进制字节流”，获取到字节流之后放在 JVM 中。Class.forName()实际上也是调用 ClassLoader 来实现的。

```java
@CallerSensitive
public static Class<?> forName(String className) throws ClassNotFoundException {
    Class<?> caller = Reflection.getCallerClass();
    return forName0(className, true, ClassLoader.getClassLoader(caller), caller);
}
```

从源码我们知道，forName() 最终调用了 forName0() 这个方法。**特别注意：第二个参数，这里默认是 true。**这个参数代表了是否对加载的类进行初始化——类中的静态代码块会被执行、静态变量会被赋值等。

```java
public static Class<?> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
    Class<?> caller = null;
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        // Reflective call to get caller class is only needed if a security manager
        // is present.  Avoid the overhead of making this call otherwise.
        caller = Reflection.getCallerClass();
        if (sun.misc.VM.isSystemDomainLoader(loader)) {
            ClassLoader ccl = ClassLoader.getClassLoader(caller);
            if (!sun.misc.VM.isSystemDomainLoader(ccl)) {
                sm.checkPermission(
                    SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
        }
    }
    return forName0(name, initialize, loader, caller);
}
```

Class 也提供可供选择的方法，我们调用方法时可以通过`initialize`来决定是否进行初始化。

应用场景：

我们使用 JDBC 时使用的是 Class.forName() 方法来加载数据库来接驱动。这是因为在 JDBC 规范中明确要求 Driver(数据库驱动)类必须向 DriverManager 注册自己。以 MySQL 的驱动为例：

```java
public class Driver extends NonRegisteringDriver implements java.sql.Driver {
    //
    // Register ourselves with the DriverManager
    //
    static {
        try {
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }

    /**
     * Construct a new driver and register it with DriverManager
     * 
     * @throws SQLException
     *             if a database error occurs.
     */
    public Driver() throws SQLException {
        // Required for Class.forName().newInstance()
    }
}
```

从代码中我们可以明白，Class.forName() 方法除了加载 Driver 类之外，静态方法中还将 Driver 类注册到 DriverManager。

Spring 的 IOC 使用的 ClassLoader 而不使用 Class.forName()，为什么呢？Spring 的 IOC 是懒加载(延迟加载)的，如果使用 Class.forName，那么懒加载这个功能就无法实现了。Spring IOC 为了加快初始化速度，因此大量使用了懒加载技术。而使用 ClassLoader 不需要执行类中的初始化代码，可以加快加载的速度，把类的初始化工作留到实际使用到这个类的时候。

### 16.2 SPI

在JDBC4.0之前，连接数据库的时候，通常会用`Class.forName("com.mysql.jdbc.Driver")`这句先加载数据库相关的驱动，然后再进行获取连接等的操作。而 JDBC4.0 之后不需要`Class.forName`来加载驱动，直接获取连接即可，这里使用了Java的SPI扩展机制来实现。

SPI 的使用参考 demo 中的 spi 部分。

```java
public static void main(String[] args) {
    ServiceLoader<Animal> animals = ServiceLoader.load(Animal.class);
    Iterator<Animal> iterator = animals.iterator();
    while (iterator.hasNext()) {
        Animal animal = iterator.next();
        animal.voice();
    } 
}
```

我们来看下 SPI 的原理。`ServiceLoader.load()` 方法必不会触发类的加载，那类在何时加载？类的加载是在遍历的时候进行的，我们看下 next() 的源码

```java
/**
 * 这 iterator 是在 ServiceLoader 中实现的
 */
public Iterator<S> iterator() {
    return new Iterator<S>() {

        Iterator<Map.Entry<String,S>> knownProviders
            = providers.entrySet().iterator();

        public boolean hasNext() {
            if (knownProviders.hasNext())
                return true;
            return lookupIterator.hasNext();
        }

        public S next() {
            if (knownProviders.hasNext())
                return knownProviders.next().getValue();
            return lookupIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    };
}

/**
 * 从源码中我们可以分析出，这个 LazyIterator 才是真实执行 Iterator 相关的方法
 */
private class LazyIterator implements Iterator<S> {
    Class<S> service;
    ClassLoader loader;
    Enumeration<URL> configs = null;
    Iterator<String> pending = null;
    String nextName = null;

    private LazyIterator(Class<S> service, ClassLoader loader) {
        this.service = service;
        this.loader = loader;
    }

    private boolean hasNextService() {
        if (nextName != null) {
            return true;
        }
        if (configs == null) {
            try {
                String fullName = PREFIX + service.getName();
                if (loader == null)
                    configs = ClassLoader.getSystemResources(fullName);
                else
                    configs = loader.getResources(fullName);
            } catch (IOException x) {
                fail(service, "Error locating configuration files", x);
            }
        }
        while ((pending == null) || !pending.hasNext()) {
            if (!configs.hasMoreElements()) {
                return false;
            }
            pending = parse(service, configs.nextElement());
        }
        nextName = pending.next();
        return true;
    }

    private S nextService() {
        if (!hasNextService())
            throw new NoSuchElementException();
        String cn = nextName;
        nextName = null;
        Class<?> c = null;
        try {
          	/**
             * 原来是调用了 Class.forName() 方法
             * 注意这里传的是 false，相当于懒加载
             */
            c = Class.forName(cn, false, loader);
        } catch (ClassNotFoundException x) {
            fail(service,
                 "Provider " + cn + " not found");
        }
        if (!service.isAssignableFrom(c)) {
            fail(service,
                 "Provider " + cn  + " not a subtype");
        }
        try {
          	// 这里进行了初始化
            S p = service.cast(c.newInstance());
          	// ServiceLoader 使用 providers(LinkedHashMap) 中保存加载类实例
            providers.put(cn, p);
            return p;
        } catch (Throwable x) {
            fail(service,
                 "Provider " + cn + " could not be instantiated",
                 x);
        }
        throw new Error();          // This cannot happen
    }

    public boolean hasNext() {
        if (acc == null) {
            return hasNextService();
        } else {
            PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
                public Boolean run() { return hasNextService(); }
            };
            return AccessController.doPrivileged(action, acc);
        }
    }

  	/**
  	 * 我们遍历是，调用 next() 方法，是调用到这里来
  	 * 该方法最终调用了 nextService() 方法
  	 */
    public S next() {
        if (acc == null) {
            return nextService();
        } else {
            PrivilegedAction<S> action = new PrivilegedAction<S>() {
                public S run() { return nextService(); }
            };
            return AccessController.doPrivileged(action, acc);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
```

