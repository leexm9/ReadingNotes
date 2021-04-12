## 第 15 章 ThreadLocal 原理

ThreadLocal 用于保存某个线程共享变量：对于同一个 static ThreadLocal，不同线程只能从中 get，set，remove 自己的变量，而不会影响其他线程的变量。

```java
public class ThreadLocal<T> {
    public void set(T value) {
      Thread t = Thread.currentThread();
      ThreadLocalMap map = getMap(t);
      if (map != null)
          map.set(this, value);
      else
          createMap(t, value);
    }

    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }
}
```

由ThreadLocal 的 set 方法的源码，可知 ThreadLocal 是把变量保存在 Thread 线程自身的 ThreadLocalMap 的属性中

```java
public class Thread implements Runnable {
    
  	/* 
     * 当前线程的ThreadLocalMap，主要存储该线程自身的ThreadLocal
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;
  
  	/*
     * InheritableThreadLocal，自父线程继承而来的ThreadLocalMap，
     * 主要用于父子线程间ThreadLocal变量的传递
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

}
```

存储的形式是以 threadLocal 自身为 key 值的 K-V 形式。

我们来分析下 ThreadLocalMap 的源码，这个是在 ThreadLocal 类中定义的

```java
static class ThreadLocalMap {
  
  // 这里的entry继承WeakReference了
  static class Entry extends WeakReference<ThreadLocal<?>> {
      Object value;
      Entry(ThreadLocal<?> k, Object v) {
          super(k);
          value = v;
      }
  }

  // 初始化容量,必须是2的n次方
  private static final int INITIAL_CAPACITY = 16;

  // entry数组,用于存储数据
  private Entry[] table;

  // map的容量
  private int size = 0;

  // 数据量达到多少进行扩容,默认是 table.length * 2 / 3
  private int threshold;
  
  private void set(ThreadLocal<?> key, Object value) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    // 采用线性探测,寻找合适的插入位置
    for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();
        // key存在则直接覆盖
        if (k == key) {
            e.value = value;
            return;
        }
        // key不存在,说明之前的ThreadLocal对象被回收了
        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    // 不存在也没有旧元素,就创建一个
    tab[i] = new Entry(key, value);
    int sz = ++size;
    // 清除旧的槽(entry不为空，但是ThreadLocal为空)，并且当数组中元素大于阈值就rehash
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
      expungeStaleEntries();
      // 扩容
      if (size >= threshold - threshold / 4)
        resize();
  }
  
}
```

从ThreadLocalMap的定义可以看出Entry的key就是ThreadLocal，而value就是值。同时，Entry也继承WeakReference，所以说Entry所对应key（ThreadLocal实例）的引用为一个弱引用。而且定义了装载因子为数组长度的三分之二。

