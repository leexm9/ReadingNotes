## 第3章 无锁CAS、Unsafe类 和及其并发包Atomic

[TOC]

### 3.1 无锁的概念

在谈论无锁概念时，总会关联起乐观派与悲观派，对于乐观派而言，他们认为事情总会往好的方向发展，总是认为坏的情况发生的概率特别小，可以无所顾忌地做事，但对于悲观派而已，他们总会认为发展事态如果不及时控制，以后就无法挽回了，即使无法挽回的局面几乎不可能发生。这两种派系映射到并发编程中就如同加锁与无锁的策略，即加锁是一种悲观策略，无锁是一种乐观策略，因为对于加锁的并发程序来说，它们总是认为每次访问共享资源时总会发生冲突，因此必须对每一次数据操作实施加锁策略。而无锁则总是假设对共享资源的访问没有冲突，线程可以不停执行，无需加锁，无需等待，一旦发现冲突，无锁策略则采用一种称为 CAS 的技术来保证线程执行的安全性，这项 CAS 技术就是无锁策略实现的关键。

### 3.2 无锁的执行者 — CAS

CAS 是 Compare and Swap 的缩写，即比较并转换。在设计并发算法的时候会用到的技术，JSR-166特性是完全建立在 CAS 的基础上的，可见其重要性。 

Java 就是通过 Unsafe 类的 compareAndSwap 系列方法实现的 CAS，当前的绝大多是 CPU 都是支持 CAS 的，不同厂商的 CPU 的 CAS 指令可能是不同的。

#### 3.2.1 CAS 原理

> 执行函数 CAS(V, E, N)

CAS 有三个操作数：内存位置 V，预期值 E 和新值 N（将要被修改成的值）。 在修改值的时候，若内存位置 V 存的值和预期值 E 相等，那么就把内存位置的 V 的值修改为 N，返回 true；若不相等，说明已经有其他线程对 V 的值做了变更，当前线程什么都不做，并返回 false。 在Java 的实现中，V 可以是一个存储 E 地址的 long 整数，E 是一个使用了 volatile 修饰的基础数据类型或者对象，N 的类型和 E 的类型一致。

#### 3.2.2 CAS 应用举例 — AtomicInteger

java.util.concurrent.atomic.AtomicInteger 是 Jdk 并发包中的一个类，如果你需要一个读写有原子性的整数类型，使用它就对了！ 我们可以看一下 AtomicInteger 的源码，可以观察到下面这个方法:

```java
/**
 * Atomically increments by one the current value
 * 
 * @return the updated value
 */
public final int incrementAndGet() {
    return unsafe.getAndAddInt(this, VALUE, 1) + 1;
}

/**
 * Unsafe类中的方法
 */
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);
    } while (!this.compareAndSetInt(o, offset, v, v + delta));
    return v;
}

public final native boolean compareAndSetInt(Object o, long offset, int expected, int x);
```

我们能够看到，为了保证操作的原子性，调用了 Unsafe 的 getAndAddInt方法，进而最终调用了 Unsafe 的 compareAndSetInt 方法。

### 3.3 Unsafe 类

我们在日常开发的时候，Java 是无法直接做操作系统级别的访问，如果想访问操作系统，我们可以使用C/C++来开发，然后使用 JNI 或者 JNA 来调用 C/C++ 的库。JVM 中存在 sun.misc.Unsafe 这样的一个类，该类中提供了一系列的底层方法，这些方法可以直接操作操作系统的内存等。该类在设计的时候，默认是不让一般的开发人员不可以使用，只有授信代码才可以使用。

##### 3.3.1 Unsafe 类是单例的

```java
public final class Unsafe {
	...
    private Unsafe() {}

    private static final Unsafe theUnsafe = new Unsafe();
    
    public static Unsafe getUnsafe() {
        return theUnsafe;
    }
    ...
}
```

##### 3.3.2 Unsafe 类的方法介绍

Unsafe类中的方法有很多:

- 操作内存的方法

  ```java
  // 分配内存指定大小的内存
  public native long allocateMemory(long bytes);
  // 根据给定的内存地址address设置重新分配指定大小的内存
  public native long reallocateMemory(long address, long bytes);
  // 用于释放allocateMemory和reallocateMemory申请的内存
  public native void freeMemory(long address);
  // 将指定对象的给定offset偏移量内存块中的所有字节设置为固定值
  public native void setMemory(Object o, long offset, long bytes, byte value);
  // 设置给定内存地址的值
  public native void putAddress(long address, long x);
  // 获取指定内存地址的值
  public native long getAddress(long address);
  
  // 设置给定内存地址的long值
  public native void putLong(long address, long x);
  // 获取指定内存地址的long值
  public native long getLong(long address);
  // 设置或获取指定内存的byte值
  public native byte getByte(long address);
  public native void putByte(long address, byte x);
  // 其他基本数据类型(long,char,float,double,short等)的操作与putByte及getByte相同
  
  // 操作系统的内存页大小
  public native int pageSize();
  ```

- 提供实例对象的新途径

  ```java
  // 传入一个对象的class并创建该实例对象，但不会调用构造方法
  public native Object allocateInstance(Class cls) throws InstantiationException;
  ```

- 类和实例对象以及变量的操作，主要方法如下

```java
// 获取字段f在实例对象中的偏移量
public native long objectFieldOffset(Field f);
// 静态属性的偏移量，用于在对应的Class对象中读写静态属性
public native long staticFieldOffset(Field f);
// 返回值就是f.getDeclaringClass()
public native Object staticFieldBase(Field f);

// 获得给定对象偏移量上的int值，所谓的偏移量可以简单理解为指针指向该变量的内存地址，
// 通过偏移量便可得到该对象的变量，进行各种操作
public native int getInt(Object o, long offset);
// 设置给定对象上偏移量的int值
public native void putInt(Object o, long offset, int x);

// 获得给定对象偏移量上的引用类型的值
public native Object getObject(Object o, long offset);
// 设置给定对象偏移量上的引用类型的值
public native void putObject(Object o, long offset, Object x);
// 其他基本数据类型(long,char,byte,float,double)的操作与getInthe及putInt相同

// 设置给定对象的int值，使用volatile语义，即设置后立马更新到内存对其他线程可见
public native void  putIntVolatile(Object o, long offset, int x);
// 获得给定对象的指定偏移量offset的int值，使用volatile语义，总能获取到最新的int值。
public native int getIntVolatile(Object o, long offset);
// 其他基本数据类型(long,char,byte,float,double)的操作与putIntVolatile及getIntVolatile相同，引用类型putObjectVolatile也一样。

// 与putIntVolatile一样，但要求被操作字段必须有volatile修饰
public native void putOrderedInt(Object o,long offset,int x);
```

- 数组操作

  ```java
  // 获取数组第一个元素的偏移地址
  public native int arrayBaseOffset(Class arrayClass);
  // 数组中一个元素占据的内存空间,arrayBaseOffset与arrayIndexScale配合使用，可定位数组中每个元素在内存中的位置
  public native int arrayIndexScale(Class arrayClass);
  ```

- CAS操作

  CAS是一些CPU直接支持的指令，也就是我们前面分析的无锁操作，在Java中无锁操作CAS基于以下3个方法实现

  ```java
  // 第一个参数o为给定对象，offset为对象内存的偏移量，通过这个偏移量迅速定位字段并设置或获取该字段的值，
  // expected表示期望值，x表示要设置的值，下面3个方法都通过CAS原子指令执行操作。
  public final native boolean compareAndSwapObject(Object o, long offset,Object expected, Object x);                                                                                                  
  public final native boolean compareAndSwapInt(Object o, long offset,int expected,int x);
  
  public final native boolean compareAndSwapLong(Object o, long offset,long expected,long x);
  ```

- 挂起与恢复

  Java 对线程的挂起操作被封装在 LockSupport 类中，LockSupport 类中有各种版本pack方法，其底层实现最终还是使用Unsafe.park()方法和Unsafe.unpark()方法

  ```java
  // 线程调用该方法，线程将一直阻塞直到超时，或者是中断条件出现  
  public native void park(boolean isAbsolute, long time);  
  
  // 终止挂起的线程，恢复正常
  public native void unpark(Object thread); 
  ```

- 内存屏障

  这些方法是在Java 8新引入的，用于定义内存屏障，避免代码重排序，与Java内存模型相关

  ```java
  // 在该方法之前的所有读操作，一定在load屏障之前执行完成
  public native void loadFence();
  // 在该方法之前的所有写操作，一定在store屏障之前执行完成
  public native void storeFence();
  // 在该方法之前的所有读写操作，一定在full屏障之前执行完成，这个内存屏障相当于上面两个的合体功能
  public native void fullFence();
  ```

- 其他操作

  ```java
  // 获取持有锁，已不建议使用
  @Deprecated
  public native void monitorEnter(Object var1);
  // 释放锁，已不建议使用
  @Deprecated
  public native void monitorExit(Object var1);
  // 尝试获取锁，已不建议使用
  @Deprecated
  public native boolean tryMonitorEnter(Object var1);
  
  // 获取本机内存的页数，这个值永远都是2的幂次方  
  public native int pageSize();  
  
  // 告诉虚拟机定义了一个没有安全检查的类，默认情况下这个类加载器和保护域来着调用者类  
  public native Class defineClass(String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain);  
  
  // 加载一个匿名类
  public native Class defineAnonymousClass(Class hostClass, byte[] data, Object[] cpPatches);
  // 判断是否需要加载一个类
  public native boolean shouldBeInitialized(Class<?> c);
  // 确保类一定被加载 
  public native  void ensureClassInitialized(Class<?> c);
  ```

### 3.4 并发包中的原子操作类—atomic

从JDK 1.5开始提供了`java.util.concurrent.atomic`包，在该包中提供了许多基于CAS实现的原子操作类，用法方便，性能高效，主要分以下4种类型。

#### 3.4.1 原子更新基本类型

原子更新基本类型主要包括3个类：

- AtomicBoolean：原子更新布尔类型
- AtomicInteger：原子更新整型
- AtomicLong：原子更新长整型

这3个类的实现原理和使用方式几乎一样，这里以 AtomicLong 为例进行源码解读

```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    // setup to use Unsafe.compareAndSwapInt for updates
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 变量value在AtomicInteger实例对象内的内存偏移量
    private static final long valueOffset;

    static {
        try {
            // 通过unsafe类的objectFieldOffset()方法，获取value变量在对象内存中的偏移
            valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
	//当前AtomicInteger 封装的int类型的值
    private volatile int value;

    ...
    
    // 设置新值并获取旧值，底层调用的是CAS操作即unsafe.compareAndSwapInt()方法
    public final int getAndSet(int newValue) {
        return unsafe.getAndSetInt(this, valueOffset, newValue);
    }
    
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    // 设置新值并获取旧值，底层调用的是CAS操作即unsafe.compareAndSwapInt()方法
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

    ...

}
```

AtomicInteger 类中getAndSet、getAndAdd等方法都调用Unsafe类中的getAndSetInt()和getAndAddInt()方法实现了CAS操作，从而保证了线程安全，通过代码发现其内部调用的是Unsafe类中的compareAndSwapInt方法，是一个CAS操作方法。

```java
public final int getAndSetInt(Object var1, long var2, int var4) {
    int var5;
    do {
        var5 = this.getIntVolatile(var1, var2);
    } while(!this.compareAndSwapInt(var1, var2, var5, var4));

    return var5;
}

public final int getAndAddInt(Object var1, long var2, int var4) {
    int var5;
    do {
        var5 = this.getIntVolatile(var1, var2);
    } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));

    return var5;
}
```

#### 3.4.2 原子更新引用

原子更新引用类型即AtomicReference原子类，该类可以保证对象的多个属性的更新具有原子性。

```java
public class AtomicReferenceDemo {

    public static AtomicReference<User> atomicUserRef = new AtomicReference<User>();

    public static void main(String[] args) {
        User user = new User("Tom", 18);
        atomicUserRef.set(user);
        User updateUser = new User("Helen", 25);
        atomicUserRef.compareAndSet(user, updateUser);
        // 执行结果:User{name='Helen', age=25}
        System.out.println(atomicUserRef.get().toString());  
    }

    static class User {
        public String name;
        private int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}
```

#### 3.4.3 原子更新数组

原子更新数组指的是通过原子的方式**更新数组里的某个元素**，主要有以下3个类

- AtomicIntegerArray：原子更新整数数组里的元素
- AtomicLongArray：原子更新长整数数组里的元素
- AtomicReferenceArray：原子更新引用类型数组里的元素

这里以AtomicIntegerArray为例进行分析，其余两个使用方式和实现原理基本一样，实例代码如下

```java
public class AtomicIntegerArrayDemo {

    private static AtomicIntegerArray integerArray = new AtomicIntegerArray(10);

    private static int[] array = new int[10];

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(new Addthread());
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println(integerArray);
        System.out.println(Arrays.toString(array));
        // 输出结果
        // [10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000]
		// [9708, 9692, 9694, 9708, 9709, 9714, 9704, 9728, 9707, 9720]
    }

    static class Addthread implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                int index = i % 10;
                integerArray.getAndIncrement(index);
                array[index]++;
            }
        }
    }

}
```

使用方式比较简单，通过源码分析，AtomicIntegerArray依然是通过调用 Unsafe 提供的方法实现的

```java
public class AtomicIntegerArray implements java.io.Serializable {
    private static final long serialVersionUID = 2862133569453604235L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 获取数组的第一个元素内存起始地址
    private static final int base = unsafe.arrayBaseOffset(int[].class);
    private static final int shift;
    // 内部数组
    private final int[] array;

    static {
        // 获取数组中一个元素占据的内存空间
        int scale = unsafe.arrayIndexScale(int[].class);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);
        return byteOffset(i);
    }
	
    // 计算数组中每个元素的的内存地址
    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private int getRaw(long offset) {
        // 获得数组中指定偏移量对应的值，使用volatile语义即总可以获取内存中最新的值
        return unsafe.getIntVolatile(array, offset);
    }

    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
    }

    public final int getAndSet(int i, int newValue) {
        return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

    public final int getAndAdd(int i, int delta) {
        return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
    }
	
    ...

}
```

#### 3.4.4 原子更新属性

如果我们只需要某个类里的某个字段具备原子操作，可以使用原子更新字段类，如在某些时候由于项目前期考虑不周全，项目需求又发生变化，使得某个类中的变量需要执行多线程操作，由于该变量多处使用，改动起来比较麻烦，而且原来使用的地方无需使用线程安全，只要求新场景需要使用时，可以借助原子更新器处理这种场景，Atomic并发包提供了以下三个类：

- AtomicIntegerFieldUpdater：原子更新整型的字段的更新器。
- AtomicLongFieldUpdater：原子更新长整型字段的更新器。
- AtomicReferenceFieldUpdater：原子更新引用类型里的字段。

请注意原子更新器的使用存在比较苛刻的条件如下

- 操作的字段不能是 static 类型。
- 操作的字段不能是 final 类型的，因为根本没法修改。
- 字段必须是 volatile 修饰的，也就是数据本身是读一致的。
- 属性必须对当前的 Updater 所在的区域是可见的，如果不是当前类内部进行原子更新器操作不能使用private，protected 子类操作父类时修饰符必须是 protected 权限及以上，如果在同一个 package 下则必须是default 权限及以上，也就是说无论何时都应该保证操作类与被操作类间的可见性。

```java
public class AtomicIntegerFieldUpdaterDemo {

    private static AtomicIntegerFieldUpdater<Candidate> atIntegerUpdater
            = AtomicIntegerFieldUpdater.newUpdater(Candidate.class, "score");

    private static AtomicReferenceFieldUpdater<Game,String> atRefUpdate =
            AtomicReferenceFieldUpdater.newUpdater(Game.class,String.class,"name");

    // 用于验证分数是否正确
    private static AtomicInteger allScore = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        final Candidate stu = new Candidate();
        Thread[] t = new Thread[10000];
        // 开启10000个线程
        for(int i = 0 ; i < 10000 ; i++) {
            t[i] = new Thread() {
                @Override
                public void run() {
                    if (Math.random( ) > 0.4){
                        atIntegerUpdater.incrementAndGet(stu);
                        allScore.incrementAndGet();
                    }
                }
            };
            t[i].start();
        }

        for (int i = 0; i < 10000; i++) {
            t[i].join();
        }
        System.out.println("最终分数score=" + stu.score);
        System.out.println("校验分数allScore=" + allScore);

        // AtomicReferenceFieldUpdater 简单的使用
        Game game = new Game(2,"zh");
        atRefUpdate.compareAndSet(game, game.name, "JAVA-HHH");
        System.out.println(game.toString());
    }

    static class Candidate {
        int id;
        volatile int score;
    }

    static class Game {
        int id;
        volatile String name;

        public Game(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Game{id = " + id + ", name = " + name + "}";
        }
    }

}
```

输出结果

```java
最终分数score=5976
校验分数allScore=5976
Game{id=2, name='JAVA-HHH'}      
```

#### 3.4.5 CAS 的 ABA 问题

ABA  问题的描述：

>1. 进程P1读取了一个数值A
>2. P1被挂起(时间耗尽、中断等)，进程P2开始执行
>3. P2修改数值A为数值B，然后又修改回A
>4. P1被唤醒，比较后发现数值A没有变化，程序继续执行。 对于线程P1来说，
>   数值一直是A未变化过，但实际上数值发生过变化的。

一般情况这种情况发现的概率比较小，可能发生了也不会造成什么问题，比如说我们对某个做加减法，不关心数字的过程，那么发生ABA问题也没啥关系。但是在某些情况下还是需要防止的，那么该如何解决呢？在Java中解决ABA问题，我们可以使用以下两个原子类

- AtomicStampedReference

  AtomicStampedReference原子类是一个带有时间戳的对象引用，在每次修改后，AtomicStampedReference不仅会设置新值而且还会记录更改的时间。当AtomicStampedReference设置对象值时，对象值以及时间戳都必须满足期望值才能写入成功，这也就解决了反复读写时，无法预知值是否已被修改的窘境，测试demo如下

  ```java
  public class ABADemo {
      static AtomicInteger atIn = new AtomicInteger(100);
  
      // 初始化时需要传入一个初始值和初始时间
      static AtomicStampedReference<Integer> atomicStampedR =
              new AtomicStampedReference<>(200,0);
  
      public static void main(String[] args) throws InterruptedException {
          Thread t1 = new Thread(new Runnable() {
              @Override
              public void run() {
                  // 更新为200
                  atIn.compareAndSet(100, 200);
                  // 更新为100
                  atIn.compareAndSet(200, 100);
              }
          });
          Thread t2 = new Thread(new Runnable() {
              @Override
              public void run() {
                  try {
                      TimeUnit.SECONDS.sleep(1);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
                  boolean flag = atIn.compareAndSet(100,500);
                  System.out.println("flag:" + flag + ", newValue:" + atIn);
              }
          });
          t1.start();
          t2.start();
          t1.join();
          t2.join();
  
          Thread t3 = new Thread(new Runnable() {
              @Override
              public void run() {
                  int time = atomicStampedR.getStamp();
                  // 更新为200
                  atomicStampedR.compareAndSet(100, 200,time,time + 1);
                  // 更新为100
                  int time2 = atomicStampedR.getStamp();
                  atomicStampedR.compareAndSet(200, 100,time2,time2 + 1);
              }
          });
          Thread t4 = new Thread(new Runnable() {
              @Override
              public void run() {
                  int time = atomicStampedR.getStamp();
                  System.out.println("sleep 前 t4 time:" + time);
                  try {
                      TimeUnit.SECONDS.sleep(1);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
                  boolean flag = atomicStampedR.compareAndSet(100,500,time,time+1);
                  System.out.println("sleep 后 t4 time:" + atomicStampedR.getStamp());
                  System.out.println("flag:" + flag + ", newValue:" + atomicStampedR.getReference());
              }
          });
          t3.start();
          t4.start();
      }
  }
  ```

  输出结果为

  ```java
  flag:true, newValue:500
  sleep 前 t4 time:0
  sleep 后 t4 time:0
  flag:false, newValue:200
  ```

  通过结果，可知 AtomicStampedReference 解决了 ABA 问题。

  ```java
  public class AtomicStampedReference<V> {
  
      private static class Pair<T> {
          final T reference;
          final int stamp;
          private Pair(T reference, int stamp) {
              this.reference = reference;
              this.stamp = stamp;
          }
          static <T> Pair<T> of(T reference, int stamp) {
              return new Pair<T>(reference, stamp);
          }
      }
  
      private volatile Pair<V> pair;
      
      public boolean compareAndSet(V   expectedReference,
                                   V   newReference,
                                   int expectedStamp,
                                   int newStamp) {
          Pair<V> current = pair;
          return
              expectedReference == current.reference &&
              expectedStamp == current.stamp &&
              ((newReference == current.reference &&
                newStamp == current.stamp) ||
               casPair(current, Pair.of(newReference, newStamp)));
      }
  
      private boolean casPair(Pair<V> cmp, Pair<V> val) {
          return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
      }
  	
      ....
  }
  ```

  通过源代码不难发现 AtomicStampedReference 内部是通过键值对 Pair 来存储数值及对应的时间戳。在更新时对数据和时间戳进行比较，只有两者都符合预期才会调用Unsafe的compareAndSwapObject方法执行数值和时间戳替换，也就避免了ABA的问题。

- AtomicMarkableReference类

  AtomicMarkableReference 源代码与 AtomicStampedReference 原理相同，不同的是AtomicMarkableReference 维护的是一个 boolean 值的标识，也就是说至于 true 和 false 两种切换状态（只有两种状态碰撞的几率就会很大），经过测试，这种方式并不能完全防止 ABA 问题的发生，只能减少 ABA 问题发生的概率。

  ```java
  public class ABADemo2 {
  
      public static  void  main(String[] args) throws InterruptedException {
          AtomicMarkableReference<Integer> atMarkRef = new AtomicMarkableReference<>(100,false);
  
          Thread t5 = new Thread(new Runnable() {
              @Override
              public void run() {
                  boolean mark = atMarkRef.isMarked();
                  System.out.println("mark:" + mark);
                  // 更新为200
                  System.out.println("t5 result:" + atMarkRef.compareAndSet(atMarkRef.getReference(), 200, mark, !mark));
              }
          });
          t5.start();
          t5.join();
  
          Thread t6 = new Thread(new Runnable() {
              @Override
              public void run() {
                  boolean mark2 = atMarkRef.isMarked();
                  System.out.println("mark2:" + mark2);
                  System.out.println("t6 result:" + atMarkRef.compareAndSet(atMarkRef.getReference(), 100, mark2, !mark2));
              }
          });
          t6.start();
          t6.join();
  
          Thread t7 = new Thread(new Runnable() {
              @Override
              public void run() {
                  boolean mark = atMarkRef.isMarked();
                  System.out.println("sleep 前 t7 mark:" + mark);
                  try {
                      TimeUnit.SECONDS.sleep(1);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
                  boolean flag = atMarkRef.compareAndSet(100,500, mark, !mark);
                  System.out.println("flag:" + flag + ", newValue:" + atMarkRef.getReference());
              }
          });
          t7.start();
      }
  }
  ```

  输出结果：

  ```java
  mark:false
  t5 result:true
  mark2:true
  t6 result:true
  sleep 前 t7 mark:false
  flag:true, newValue:500
  ```