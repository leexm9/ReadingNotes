## 第 5 章  synchronized 和 ReentrantLock

[TOC]

ReentrantLock 是基于 AQS 设计的可重入锁，synchronized 是基于对象监视器实现的可重入锁。使用了它们后，代码都会具有原子性（atomicity）和可见性 （visibility）。
可重入锁也被称为递归锁，指同一个线程内，外层代码锁未被释放时，内层代码也可以获取到锁，递归就是一种很常见的场景。下面就是可重入锁的一种使用场景：

```java
public class Demo {
    Lock lock = new Lock();
    public void outer(){
    	lock.lock();
        inner();
        lock.unlock();
    }				
    public void inner(){
        lock.lock();
        //do something 
        lock.unlock();
    } 
}
```

ReentrantLock 和 synchronized 在并发编程中，有着相同的语义，但是它们实现的原理存在着较大的差异，在设计的思想上更是有着很多不同之处。

### 5.1 synchronized 语义原理

关键字 synchronized 的作用是实现线程同步。它的工作是对同步的代码加锁，使得每次只能有一个线程进入同步代码，从而保证了线程间的安全。synchronized 的用法有如下几种：

- 对象级别加锁：相当于对当前实例对象加锁，进入同步代码块前要获得当前实例的锁

  ```java
  //变量 
  int	count = 0; 
  synchronized(count){
      //... 
  }
  // 实例方法
  public synchronized	void method(){
      //... 
  }
  // 当前对象实例
  synchronized(this){
      //... 
  }
  ```

- 类级别加锁：相当于对当前类加锁，进入同步代码前要获得当前类的锁

  ```java
  // 共享变量，
  static int count = 0;
  // 获取共享变量的锁
  synchronized(count){
      //... 
  }
  // 类方法
  public synchronized	static void method(){
      //... 
  }
  // 类加载器
  synchronized(Demo2.class){
      //... 
  } 
  // 或者
  synchronized(this.getClass()){
      //...
  }
  ```

#### 5.1.1 理解 Java 对象头和 Monitor

在 JVM 中，对象在内存中的布局分为三块区域：对象头、实例变量和填充数据，如图：

![](./pictures/heap1.jpg)

- 实例变量：存放类的属性数据信息，包括父类的属性信息，如果是数组的实例部分还包括数组的长度，这部分内存按4字节对齐。
- 填充数据：由于虚拟机要求对象起始地址必须是8字节的整数倍。填充数据不是必须存在的，仅仅是为了字节对齐，这点了解即可。

而对于 Java 的对象头，它是实现 synchronized 的锁对象的基础。一般而言，synchronized 使用的锁对象是存储在 Java 对象头里的，JVM 中采用2个字节存储对象头（如果对象是数组类型则会分配3个字节，多出的一个字节记录数组的长度），其主要结构是由 ***Mark Word*** 和 ***Class Metadata Address*** 组成：

| 虚拟机位数 |       头对象结构       |                             说明                             |
| :--------: | :--------------------: | :----------------------------------------------------------: |
|  32/64bit  |       Mark Word        |      存储对象的 hashcode、锁、分代年龄或者GC标志等信息       |
|  32/64bit  | Class Metadata Address | 类型指针，指向对象的类元数据，JVM 通过这个指针确定该对象是哪个类的实例 |
|  32/64bit  |      Array Length      |              数组的长度，如果当前对象是数组的话              |

其中Mark Word在默认情况下存储着对象的 hashCode、分代年龄、锁标记位等信息，以下是32位虚拟机的 Mark Word 默认存储结构：

|  锁状态  |     25bit     |     4bit     | 1bit是否是偏向锁 | 2bit 锁标志位 |
| :------: | :-----------: | :----------: | :--------------: | :-----------: |
| 无锁状态 | 对象 hashCode | 对象分代年龄 |        0         |      01       |

在运行期间，Mark Word 里存储的数据会随着锁标志位的变化而变化。Mark Word 可能变化存储为以下4种数据结构：![](./pictures/markword1.jpg)

在64位虚拟机，Mark Word 是64bit大小，其存储结构：![64虚拟机中Mark Word](./pictures/markword2.jpg)

其中轻量级锁和偏向锁是 Java 6 为了减少获得锁和释放锁带来的性能消耗引入的。我们重点分析下重量级锁也就是通常所说的 synchronized 的对象锁，锁标志位为10，其中指针指向的 monitor 对象（也称为管程或监视器锁）的起始地址。每个对象都存在着一个 monitor 对象与之关联，对象与其 monitor 之间的关系存在多种实现方式，如 monitor 可以与对象一起创建和销毁，或者当线程试图获取对象锁时自动生成，当一个 monitor 被某个线程持有后，它便处于锁定状态。在 Java 虚拟机中，monitor 是由 ObjectMonitor 实现的，其主要数据结构如下（位于 HotSpot 虚拟机源码 ObjectMonitor.hpp 文件，C++实现的）：
```c++
ObjectMonitor() {
    _header       = NULL;
    _count        = 0; //记录个数
    _waiters      = 0,
    _recursions   = 0;
    _object       = NULL;
    _owner        = NULL;
    _WaitSet      = NULL; //处于wait状态的线程，会被加入到_WaitSet
    _WaitSetLock  = 0;
    _Responsible  = NULL;
    _succ         = NULL;
    _cxq          = NULL;
    FreeNext      = NULL;
    _EntryList    = NULL; //处于等待锁block状态的线程，会被加入到该列表
    _SpinFreq     = 0;
    _SpinClock    = 0;
    OwnerIsThread = 0;
  }
```

ObjectMonitor 中有两个队列，***_WaitSet*** 和 ***_EntryList***，用来保存 ObjectWaiter 对象列表（每个等待锁的线程都会被封装成 ObjectWaiter 对象），***_Owner*** 指向持有 ObjectMonitor 对象的线程。当多个线程同时访问同一段同步代码时，会首先进入 ***_EntryList*** 集合，当线程获取到对象的 monitor 后进入到 _Owner 区域并把 monitor 中的 owner 变量设置为当前线程同时 monitor 中的计数器 count 加1；若线程调用 wait() 方法，将释放当前持有的 monitor，owner 变量恢复为 null，count 自减1，同时该线程进入 _WaitSet 集合中等待被唤醒。若当前线程执行完毕也将释放 monitor 并复位变量的值，以便其他线程进入获取 monitor。过程如下图所示：![](./pictures/markword3.jpg)

可以看出，monitor 对象存在于每个 Java 对象的对象头中（存储的指针的指向），synchronized 锁便是通过这种方式获取锁的，也是为什么 Java 中任意对象可以作为锁使用的原因，同时也是 notify/notifyAll/wait 等方法存在于顶级对象 Object 中的原因。

#### 5.1.2 synchronized 的底层原理

```java
/**
 * 同步代码块
 */
public void method1() {
    synchronized(this) {
        System.out.println("method1 start");
    }
}

/**
 * 同步方法
 */
public synchronized void method2() {
    System.out.println("method2 start");
}
```

反编译上述代码，我们可以看到字节码如下：

```java
public void method1();
    descriptor: ()V
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=1
         0: aload_0
         1: dup
         2: astore_1
         3: monitorenter
         4: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
         7: ldc           #5                  // String method1 start
         9: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        12: aload_1
        13: monitorexit
        14: goto          22
        17: astore_2
        18: aload_1
        19: monitorexit
        20: aload_2
        21: athrow
        22: return
      Exception table:
         from    to  target type
             4    14    17   any
            17    20    17   any
      LineNumberTable:
        line 7: 0
        line 8: 4
        line 9: 12
        line 10: 22
      StackMapTable: number_of_entries = 2
        frame_type = 255 /* full_frame */
          offset_delta = 17
          locals = [ class SyncTest, class java/lang/Object ]
          stack = [ class java/lang/Throwable ]
        frame_type = 250 /* chop */
          offset_delta = 4
          
public synchronized void method2();
    descriptor: ()V
    flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED
    Code:
      stack=2, locals=1, args_size=1
         0: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #6                  // String method2 start
         5: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: return
      LineNumberTable:
        line 13: 0
        line 14: 8
```

通过字节码我们可以知道，同步代码块的实现使用的是 ***monitorenter*** 和 ***monitorexit*** 指令。

关于 monitorenter

- 若 monitor 的计数器得值为0，则该线程成功获得 monitor，然后将计数器得值设置为1，该线程成为了monitor 的所有者，获取锁成功； 
- 若monitor 的计数器得值不为0，且该线程已经拥有了 monitor 的所有权，该线程只是重新进入该代码块，重入时计数器的值加1； 
- 若 monitor 的计数器的值不为0，且其它线程已经占用了 monitor，则该线程进入阻塞状态，直到 monitor 的计数器的值为0时，再重新尝试获取 monitor； 

> 值得注意的是编译器将会确保无论方法以何种方式完成，方法中调用过的每条 monitorenter 指令都有执行其对应 monitorexit 指令，而无论这个方法是正常结束还是异常结束。为了保证在方法异常完成时 monitorenter 和 monitorexit 指令依然可以正确配对执行，编译器会自动产生一个异常处理器，这个异常处理器声明可处理所有的异常，它的目的就是用来执行 monitorexit 指令。从字节码中也可以看出多了一个monitorexit 指令，它就是异常结束时被执行的释放 monitor 的指令。

关于monitorexit 

- 指令执行的时候，monitor 的计数器的值减1，当减1后为计数器的值为 0 的时候，线程将释放 monitor。其它被这个 monitor 阻塞的线程开始尝试获取该 monitor 的所有权；
- object 的 wait/notify 方法是依赖 monitor 的，所以只能在同步代码块或者方法中才能调用 wait/notify 等方法，否则会抛出异常。	执行 monitorexit 必须是某个已经取得 monitor 所有权的线程； 

synchronized 同步方法（method2）

从字节码中可以看出，synchronized 修饰的方法并没有 monitorenter 指令和 monitorexit 指令，取而代之的确实是 ***ACC_SYNCHRONIZED*** 标识，该标识指明了该方法是一个同步方法，虚拟机通过该标识来辨别一个方法是否为同步方法，若是，则进入方法的时候会做和 monitorenter 一样的事情，当退出方法的时候将会作出和 monitorexit 一样的事情。

### 5.2 锁的升级和对比

Java 6 为了减少获得锁和释放锁带来的性能消耗，引入了“偏向锁”和“轻量级锁”。在Java 6 中，锁一共有4中状态，级别从低到高依次是：无锁状态、偏向锁状态、轻量级锁状态和重量级锁状态，这几个状态会随着竞争情况逐渐升级。锁可以升级但不能降级，这种升级却不能降级的策略，目的是为了提高获得锁和释放锁的效率。

#### 5.2.1 偏向锁

研究发现，大多数情况下锁不仅不存在多线程竞争，而且总是由同一个线程多次获得，为了让线程获得锁的代价更低而引入了偏向锁。当一个线程访问同步代码块并获得锁时，会在对象头和栈帧中的锁记录存储锁偏向的线程ID，以后该线程在进入和退出同步代码块时不需要进行 CAS 操作来加锁和解锁，只需要简单地测试下对象头的 Mark Word 里是否存储着指向当前线程的偏向锁。如果测试成功，表示线程已经获得了锁。如果测试失败，则需要再测试下 Mark Word 中偏向锁的标识是否设置成1（表示当前是偏向锁）：如果没有设置，则使用 CAS 竞争锁；如果设置了，则尝试使用 CAS 将对象头的偏向锁指向当前线程。

- 偏向锁的撤销：

  偏向锁使用一种等到竞争出现才释放的机制，所以当其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放偏向锁。偏向锁的撤销，需要等到全局安全点（这个时间点上没有正在执行的字节码）。它会首先暂停拥有偏向锁的线程，然后检查持有偏向锁的线程是否还活着，如果线程不处于活动状态，则将对象头设置成无锁状态；如果线程仍然活着，拥有偏向锁的线程会被执行，遍历偏向对象的锁记录，栈中的锁记录和对象头的 Mark Word 要么重新偏向于其他线程，要么恢复到无锁或者标记对象不适合作为偏向锁，最后唤醒暂停的线程。下图中线程1演示了偏向锁的初始化的流程，线程2演示了偏向锁的撤销流程：


  ![](./pictures/synchronized1.jpg)

#### 5.2.2 轻量级锁

- 轻量级锁加锁

  线程在执行同步代码块之前，虚拟机会先在当前线程的栈帧中创建用于存储锁记录的空间，并将对象头中的 Mark Word 复制到锁记录中（官方称之为 Displaced Mark Word）。然后线程尝试使用 CAS 将对象头记录中的 Mark Word 替换为指向锁记录的指针。如果成功，当前线程获得锁；如果失败，表示其他线程竞争锁，当前线程便尝试使用自旋来获取锁。

- 轻量级锁解锁

  轻量级锁解锁时，会使用原子的 CAS 操作将 Displaced Mark Word 替换回对象头，如果成功，则表示没有竞争发生。如果失败，表示当前锁存在竞争，锁就会膨胀成重量级锁。

  ![](./pictures/synchronized2.jpg)

  因为锁的自旋会消耗 CPU，为了避免无用的自旋（比如获得锁的线程被阻塞了），一旦锁升级成重量级锁，就不会恢复到轻量级锁状态。当锁处于这个状态下，其他线程试图获取锁时，都会被阻塞住，当持有锁的线程释放锁之后会唤醒这些线程，被唤醒的线程就会进行新一轮的锁的竞争。

#### 5.2.3 锁竞争的优缺点对比

| 锁   | 优点 | 缺点 | 适用场景 |
| :----: | :----: | :----: | :--------: |
| 偏向锁 | 加锁和解锁不需要额外的消耗，和执行非同步代码相比仅存在纳秒级的差距 | 如果线程间存在锁竞争，会带来额外的锁撤销的消耗 | 适用于只有一个线程访问同步代码块场景 |
| 轻量级锁 | 竞争的线程不会阻塞，提高了程序的响应速度 | 如果始终得不到锁的竞争线程，适用自旋会消耗CPU | 追求响应时间，同步代码块执行速度非常快 |
| 重量级锁 | 线程竞争不使用自旋，不会消耗CPU | 线程阻塞，响应速度缓慢 | 追求吞吐量，同步代码块执行时间较长 |

### 5.3 ReentrantLock 解析

#### 5.3.1 Lock 接口

前面我们详谈过解决多线程同步问题的关键字 synchronized，synchronized 属于隐式锁，即锁的持有与释放都是隐式的，我们无需干预。在Java 1.5中，官方在 concurrent 并发包中加入了 Lock 接口，该接口中提供了 lock() 方法和 unLock() 方法对显式加锁和显式释放锁操作进行支持，简单了解一下代码编写，如下：

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    //临界区......
} finally {
    lock.unlock();
}
```

在 finally 块中释放锁，保证在获取锁之后最终能够释放锁。不要将获取锁的过程写在 try 块中，因为如果在获取锁时发生异常，异常抛出的同时，也会导致锁无故释放。

Lock 接口提供了 synchronized 不具备的特性：

- 尝试非阻塞地获取锁：当前线程尝试获取锁，如果这一时刻锁没有被其他线程获取到，则成功获取并持有锁
- 能被中断地获取锁：与 synchronized 不同，获取到锁的线程能够响应中断，当获取到锁的线程被中断时，中断异常将会抛出，同时锁会释放
- 超时获取：在指定的截止时间之前获取锁，如果截止时间到了仍旧无法获取锁，则返回

Lock 的 API 接口：

| 方法名 | 描述 |
| :--: | :--:|
| void lock() | 获取锁，调用该方法的当前线程将会获取锁，当锁获得后，从该方法返回 |
| void lockInterruptibly() throws InterruptedException | 可中断地获取锁，和 lock() 方法的不同之处在于该方法会响应中断，即在锁的获取中可以中断当前线程 |
| boolean tryLock() | 尝试非阻塞的获取锁，调用该方法后立刻返回，如果能够获取则返回 true，反之返回 false |
| boolean tryLock(Long time, TimeUnit unit) throws InterruptedException | 超时获取锁，当前线程在一下3种情况下会返回：<br/>1、当前线程在超时时间内获得了锁<br/>2、当前线程在超时时间内被中断了<br/>3、超时时间结束，返回false |
| void unLock() | 释放锁 |
| Condition new Condition() | 获取等待通知组件，该组件和当前的锁绑定，当前线程只有获得了锁，才能调用该组件的 wait() 方法；而调用后，当前线程将释放锁 |

#### 5.3.2 ReetrantLock 重入锁

ReetrantLock本身也是一种支持重进入的锁，即该锁可以支持一个线程对资源重复加锁，同时也支持公平锁与非公平锁。阅读 ReentrantLock源码时主要就是查看它三个静态内部类的实现	，以及***公平锁***和***非公平锁***的实现差异。

***内部类***：

主要有这三个静态内部 类 java.util.concurrent.locks.ReentrantLock.Sync 、java.util.concur rent.locks.ReentrantLock.NonfairSync 以 及 java.util.concurrent.locks.ReentrantLock.FairSync。Sync 类是另外两个的父类，NonfairSync 类实现的是非公平锁，FairSync 类实现的是公平锁。

- Sync

  Sync类是一个抽象类，它主要声明了 lock 抽象方法，实现了获取非公平锁的方法 nonfairTryAcquire，以及释放锁的方法 tryRelease。

  ```java
  abstract static class Sync extends AbstractQueuedSynchronizer {
      private static final long serialVersionUID = -5179523762034025860L;
  
      abstract void lock();	// 子类需要实现该方法
  
      /**
       * 尝试获取非公平锁
       */
      final boolean nonfairTryAcquire(int acquires) {
          final Thread current = Thread.currentThread();
          int c = getState();
          if (c == 0) {
              // 重入次数是0，锁未被占用，则直接占用该锁
              if (compareAndSetState(0, acquires)) {
                  setExclusiveOwnerThread(current);
                  return true;
              }
          }
          else if (current == getExclusiveOwnerThread()) { // 锁已经被当前线程占用
              int nextc = c + acquires;	// 重入次数 + acquires
              if (nextc < 0) // 重入次数超出 int 类型的范围
                  throw new Error("Maximum lock count exceeded");
              setState(nextc);	// 更新重入次数
              return true;
          }
          // 锁已经被其他线程占用
          return false;
      }
  
      /**
       * 尝试释放锁，当重入计数器 state 值变为0后，表示以及没有锁的占用了
  	 */
      protected final boolean tryRelease(int releases) {
          int c = getState() - releases;
          // 判断当前线程是否持有锁
          if (Thread.currentThread() != getExclusiveOwnerThread())
              throw new IllegalMonitorStateException();
          boolean free = false;
          if (c == 0) {	// 检查重入计数器的值是否为0，是则将锁的持有者置空
              free = true;
              setExclusiveOwnerThread(null);
          }
          setState(c);	// 更新重入计数器
          return free;
      }
  
      /**
       * 判断当前线程是否持有该锁
       */
      protected final boolean isHeldExclusively() {
          return getExclusiveOwnerThread() == Thread.currentThread();
      }
  
      final ConditionObject newCondition() {
          return new ConditionObject();
      }
  
      /**
       * 获取持有该锁的线程
       */
      final Thread getOwner() {
          return getState() == 0 ? null : getExclusiveOwnerThread();
      }
  
      /**
       * 获得当前线程重入该锁的次数
       */
      final int getHoldCount() {
          return isHeldExclusively() ? getState() : 0;
      }
  
      /**
     	 * 判断该锁是否被占用
       */
      final boolean isLocked() {
          return getState() != 0;
      }
  
      /**
       * Reconstitutes the instance from a stream (that is, deserializes it).
       */
      private void readObject(java.io.ObjectInputStream s)
          throws java.io.IOException, ClassNotFoundException {
          s.defaultReadObject();
          setState(0); // reset to unlocked state
      }
  }
  ```

- NonfairSync

  非公平锁的实现，主要是实现了lock方法。

  ```java
  static final class NonfairSync extends Sync {
      private static final long serialVersionUID = 7316153563782823691L;
  
      /**
       * 获取锁，判断下锁是否已经被其他线程持有，没有则当前线程直接占用该锁；否则，进入阻塞
       */
      final void lock() {
          if (compareAndSetState(0, 1))	// CAS 判断锁是否已经被占用
              // 没有
              setExclusiveOwnerThread(Thread.currentThread());	// 直接占用该锁
          else
              acquire(1);		// 锁已经被其他线程持有，调用 AQS 尝试获取锁以及进入阻塞
      }
  
      protected final boolean tryAcquire(int acquires) {
          return nonfairTryAcquire(acquires);
      }
  }
  ```

- FairSync

  公平锁的实现

  ```java
  static final class FairSync extends Sync {
      private static final long serialVersionUID = -3000897897090466540L;
  	
      /**
       * 由于需要判断是否公平，所以和 NonfairSync#lock() 的实现稍有不同，并没有在 AQS 的 state
       * 值为0时，立马获取到锁
       */
      final void lock() {
          acquire(1);
      }
  
      /**
       * 除了多调用了 hasQueuedPredecessors 方法外，其它和 nonfairTryAcquire 几乎一样
       */
      protected final boolean tryAcquire(int acquires) {
          final Thread current = Thread.currentThread();
          int c = getState();		// 获取到重入次数
          if (c == 0) {
              /**
               * 查看是否有比当前线程等待更久的线程（即当前线程节点是否有前置节点），有就返回 true 				* 没有就返回 false，和 nonfairTryAcquire 相比，只多出了这一块
               */
              if (!hasQueuedPredecessors() &&
                  compareAndSetState(0, acquires)) {
                  setExclusiveOwnerThread(current);
                  return true;
              }
          }
          else if (current == getExclusiveOwnerThread()) {
              int nextc = c + acquires;
              if (nextc < 0)
                  throw new Error("Maximum lock count exceeded");
              setState(nextc);
              return true;
          }
          return false;
      }
  }
  ```

