## 第 12 章 Java 线程池异常处理机制和退出关闭机制

### 12.1 Thread 中异常的处理

首先看下 Thread 线程的异常处理，我们都知道 Runnable 接口中的 run() 方法是不允许抛出异常的，需要我们自己进行 try/catch 进行处理。如果我们不 try/catch 会如何？

```java
public class ThreadException {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> System.out.println(1 / 0));
      	// thread.setUncaughtExceptionHandler((t, e) -> System.out.println(String.format("Exception:%s", e.getMessage())));
        thread.start();
        thread.join();
        System.out.println("Hello, world");
    }

}
```

上述代码的输出是：

```java
Exception in thread "Thread-0" java.lang.ArithmeticException: / by zero
	at com.leexm.test.thread.ThreadException.lambda$main$0(ThreadException.java:14)
	at java.lang.Thread.run(Thread.java:748)
Hello, world
```

把中间的 `setUncaughtExceptionHandler`注释去除，输出是

```java
Exception:/ by zero
Hello, world
```

由上面的例子，我们除了 `try/catch` 方式外，还可以通过`setUncaughtExceptionHandler(UncaughtExceptionHandler eh)`来设置 `UncaughtExceptionHandler`来处理异常。

我们来看下 Thread 中涉及到具体方法：

```java
public class Thread implements Runnable {	
  
  	/* The group of this thread */
    private ThreadGroup group;
  
  	// null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;	
  	
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }
  
  	public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setDefaultUncaughtExceptionHandler"));
        }
        defaultUncaughtExceptionHandler = eh;
     }
  
  	/**
  	 * JVM 会调用这个方法来处理 run 方法抛出的异常
  	 */
  	private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }
  
  	/**
  	 * 如果设置的 uncaughtExceptionHandler， 则调用它来处理
  	 * 如果 thread 没有设置，则交由 threadGroup 来处理
  	 */
  	public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ? uncaughtExceptionHandler : group;
    }
}

public class ThreadGroup implements Thread.UncaughtExceptionHandler {
  
    public void uncaughtException(Thread t, Throwable e) {
        if (parent != null) {
          parent.uncaughtException(t, e);
        } else {
          	/**
          	 * 交由 Thread 的默认 defaultUncaughtExceptionHandle 来处理
          	 * 如果没有设置默认的 handler，则使用 System.err 来输出
          	 * Tomcat 的 System.err 的输出是 catalina.out
          	 */
            Thread.UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            } else if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception in thread \"" + t.getName() + "\" ");
                e.printStackTrace(System.err);
            }
        }
    }
 
}
```

异常的处理涉及三个属性：`uncaughtExceptionHandler`、`defaultUncaughtExceptionHandler` 和 `group`，前两个好理解，`uncaughtExceptionHandler` 是用来给具体 Thread 实例设置的，`defaultUncaughtExceptionHandler`是给 Thread 这个类设置默认的异常处理，该类的所有实例也将继承这个异常处理 `handler`。对于 `ThreadGroup`，我们需要知道每个线程都有自己的 `ThreadGroup`，即使没有显式指定也会从创建该线程父线程处得到。

现在的问题是如何触发的 `handler` 处理异常的？查阅 JVM 资料发现 JVM 在遇到 `run()` 方法抛出异常的情况下，会调用 `dispatchUncaughtException()`方法来处理

### 12.2 线程池中异常的处理

```java
public class ThreadException {

    public static void main(String[] args) throws InterruptedException {

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
        ThreadPoolExecutor executors = new ThreadPoolExecutor(1, 3, 50, TimeUnit.MILLISECONDS, queue);

        executors.execute(() -> {
            Object obj = null;
            System.out.println(obj.toString());
        });
        TimeUnit.SECONDS.sleep(1);
        System.out.println("main-1");

        executors.submit(() -> Integer.valueOf("ab"));
        TimeUnit.SECONDS.sleep(1);
        System.out.println("main-2");

        Future<Integer> future = executors.submit(() -> {
            System.out.println(1 / 0);
            return 100;
        });
        try {
            System.out.println(future.get());
        } catch (ExecutionException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("main-3");

        executors.shutdownNow();
    }

}
```

输出结果是：

```java
Exception in thread "pool-1-thread-1" java.lang.NullPointerException
	at com.leexm.test.thread.ThreadException.lambda$main$0(ThreadException.java:42)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
main-1
main-2
java.lang.ArithmeticException: / by zero
main-3
```

从上述的输出结果，我们可以知道通过 `submit()`方法提交的任务，执行遇到异常没有任何的输出，`execute()`方法却在 console 中打印了输出，为什么存在这个差异？

`execute()`方法控制台的输出，和上面 thread 的输出非常像，应该是运行到了`dispatchUncaughtException(...)`方法了，我们通过 debug 打断点，可以确认这一点。

我们先来看看，线程池如何处理 `submit` 提交的任务

```java
// 以下的这些方法都是 AbstractExecutorService 中的方法
public Future<?> submit(Runnable task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
}

public <T> Future<T> submit(Runnable task, T result) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task, result);
    execute(ftask);
    return ftask;
}

public <T> Future<T> submit(Callable<T> task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task);
    execute(ftask);
    return ftask;
}

protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
  return new FutureTask<T>(runnable, value);
}
```

根据上述方法，我们知道通过 `submit` 提交的任务，都会转换成 `RunableFuture`在提交到线程池。线程池中处理提交的任务的方法是 `runWorker(Worker w)`(线程池的原理就不讨论了)

```java
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;
    w.unlock();
    boolean completedAbruptly = true;
    try {
        while (task != null || (task = getTask()) != null) {
          w.lock();
          if ((runStateAtLeast(ctl.get(), STOP) || 
               (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted())
            wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                  	// 最终调用的是任务的 run 方法
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                  	// 不论是执行哪种任务，都会执行这个方法，我们可以通过重写这个方法来定义异常处理逻辑
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
      	processWorkerExit(w, completedAbruptly);
    }
}
```

`task.run()`如果抛出异常，都会被 catch 住，从而触发 Thread 的`dispatchUncaughtException(...)`的调用。

接下来我们看看 `FutureTask`的 `run`方法的逻辑：

```java
public class FutureTask<V> implements RunnableFuture<V> {
   
    private Callable<V> callable;

    private Object outcome; // non-volatile, protected by state reads/write

    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    public void run() {
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                  	// 注意：这个地方是我们考察的关键，对于捕获的异常，FutureTask 并没有往上抛出
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            runner = null;
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }
  
  	/**
  	 * 将异常对应赋值给了 outcome 这个成员变量
  	 */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }
  
  	@SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
       	// 任务正常执行完成，则 outcome 就是需要返回的结果
      	if (s == NORMAL)
            return (V)x;
      	// 任务被取消，则返回 CancellationException
        if (s >= CANCELLED)
            throw new CancellationException();
      	// 如果是其他情况导致任务异常，outcome 就是在 run 方法中 catch 的 throwable
        throw new ExecutionException((Throwable)x);
    }
	
  	public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }
}
```

这里最关键的地方就是将异常对象赋值给了 outcome，outcome 是 FutureTask 中的成员变量，我们通过调用 submit 方法，拿到一个 Future 对象之后，再调用它的 get 方法，其中最核心的方法就是 report 方法。

***结论***：`FutureTask`的`run`方法在被线程池调用时是不会抛出异常的，异常有可能被线程池吃掉。

综上所述，针对提交给线程池的任务可能会抛出异常这一问题，主要有以下两种处理思路：

- 在提交的任务当中自行try...catch，但这里有个不好的地方就是如果你会提交多种类型的任务到线程池中，每种类型的任务都需要自行将异常try...catch住，比较繁琐。而且如果你只是`catch(Exception e)`，可能依然会漏掉一些包括Error类型的异常，那为了保险起见，你可以考虑`catch(Throwable t)`。
- 自行实现线程池的`afterExecute`方法，或者实现`Thread`的`UncaughtExceptionHandler`接口

```java
public class ThreadException {

    public static void main(String[] args) throws InterruptedException {

        Logger logger = LoggerFactory.getLogger(ThreadException.class);

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);

        ThreadPoolExecutor executors = new ThreadPoolExecutor(1, 3,
                50, TimeUnit.MILLISECONDS, queue, new ThreadFactory() {
            private int count = 0;
            private String prefix = "Test";

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, prefix + "-" + count++);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    logger.error("Test thread:{} exception:", t.getName(), e);
                });
                return thread;
            }
        }) {

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t == null && r instanceof Future<?>) {
                    Future<?> future = (Future<?>) r;
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        t = e;
                    } catch (ExecutionException e) {
                        t = e;
                    }
                }
                if (t != null) {
                    logger.error("executors error msg:{}.", t.getCause().toString());
                }
            }

        };
      	// 将所有核心线程创建起来
        executors.prestartAllCoreThreads();

        executors.execute(() -> {
            Object obj = null;
            System.out.println(obj.toString());
        });
        TimeUnit.SECONDS.sleep(1);
        System.out.println("main-1");

        executors.submit(() -> Integer.valueOf("ab"));
        TimeUnit.SECONDS.sleep(1);
        System.out.println("main-2");

        Future<Integer> future = executors.submit(() -> {
            System.out.println(1 / 0);
            return 100;
        });
        try {
            System.out.println(future.get());
        } catch (ExecutionException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("main-3");

        executors.shutdownNow();
    }

}
```

### 12.3 线程数的设置

我们知道任务一般有两种：CPU密集型和IO密集型。那么面对CPU密集型的任务，线程数不宜过多，一般选择CPU核心数+1或者核心数的2倍是比较合理的一个值。因此我们可以考虑将corePoolSize设置为CPU核心数+1，maxPoolSize设置为核心数的2倍。那么同样的，面对IO密集型任务时，我们可以考虑以核心数乘以4倍作为核心线程数，然后核心数乘以5倍作为最大线程数的方式去设置线程数，这样的设置会比直接拍脑袋设置一个值会更合理一些。

当然总的线程数不宜过多，控制在100个线程以内比较合理，否则线程数过多可能会导致频繁地上下文切换，导致系统性能反不如前。

### 12.4 如何正确关闭一个线程池

说到如何正确去关闭一个线程池，这里面也有点讲究。为了实现优雅停机的目标，我们应当先调用shutdown方法，调用这个方法也就意味着，这个线程池不会再接收任何新的任务，但是已经提交的任务还会继续执行，包括队列中的。所以，之后你还应当调用awaitTermination方法，这个方法可以设定线程池在关闭之前的最大超时时间，如果在超时时间结束之前线程池能够正常关闭，这个方法会返回true，否则，一旦超时，就会返回false。通常来说我们不可能无限制地等待下去，因此需要我们事先预估一个合理的超时时间，然后去使用这个方法。

如果awaitTermination方法返回false，你又希望尽可能在线程池关闭之后再做其他资源回收工作，你可以考虑再调用一下shutdownNow方法，此时队列中所有尚未被处理的任务都会被丢弃，同时会设置线程池中每个线程的中断标志位。shutdownNow并不保证一定可以让正在运行的线程停止工作，除非提交给线程的任务能够正确响应中断。到了这一步，你可以考虑继续调用awaitTermination方法，或者你直接放弃，去做接下来要做的事情。

```java
// 退出线程池的标准做法
ExecutorService executors = Executors.newFixedThreadPool(3);
...
executors.shutdown();
try {
    if(!executors.awaitTermination(60, TimeUnit.SECONDS)){
      // 超时的时候向线程池中所有的线程发出中断(interrupted)。
      executors.shutdownNow();
    }
} catch (InterruptedException ignore) {
  	executors.shutdownNow();
}
```

#### 12.4.1 线程中断

在介绍线程池中断之前，我们先来看下线程的中断机制。

在程序中，我们是不能随便中断一个线程的，因为这是极其不安全的操作，我们无法知道这个线程正运行在什么状态，它可能持有某把锁，强行中断可能导致锁不能释放的问题；或者线程可能在操作数据库，强行中断导致数据不一致混乱的问题。正因此，JAVA里将 `Thread` 的 `stop` 方法设置为过时，以禁止大家使用。

一个线程什么时候可以退出呢？当然只有线程自己才能知道。

所以我们这里要说的 Thread 的 `interrrupt()` 方法，本质不是用来中断一个线程而是将线程设置一个中断标记位，有两个作用：

- 如果此线程处于阻塞状态(比如调用了wait方法，io等待)，则会立马退出阻塞，并抛出 `InterruptedException` 异常，线程就可以通过捕获 `InterruptedException` 来做一定的处理，然后让线程退出。
- 如果此线程正处于运行之中，则线程不受任何影响，继续运行，仅仅是线程的中断标记被设置为true。所以线程要在适当的位置通过调用 `isInterrupted()` 方法来查看自己是否被中断，并做退出操作。

#### 12.4.2 线程池退出

线程池提供了两个关闭方法，`shutdownNow()  `和 `shuwdown()` 方法。

##### shoudownNow 方法

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
      	// 将线程的状态，原子性修改为 STOP
        advanceRunState(STOP);
        interruptWorkers();
      	/**
      	 * 跟踪代码，最终会调用队列的 drainTo() 方法，移除队列中的所有等待任务
         * 将已提交的还没有执行的任务，返回给调用方，调用方可以把这些任务日志输出、存储起来等
         */
        tasks = drainQueue();
    } finally {
      	mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}

/**
 * 遍历线程池中的线程，调用线程的 interrupt 方法，设置线程的中断标志位
 */
private void interruptWorkers() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        for (Worker w : workers)
            w.interruptIfStarted();
    } finally {
        mainLock.unlock();
    }
}

private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    // 删除无关方法
    void interruptIfStarted() {
        Thread t;
        if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
            try {
              	t.interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }
}
```

那么调用`shutdownNow()` 方法后，线程池会如何反应？

```java
 final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;
    w.unlock();
    boolean completedAbruptly = true;
    try {
      	/**
      	 * 这里 getTask() 方法返回 null，会导致这个 while 循环退出，进而导致线程池中的线程退出
      	 */
        while (task != null || (task = getTask()) != null) {
          w.lock();
          if ((runStateAtLeast(ctl.get(), STOP) || 
               (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted())
            wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                  	/**
                  	 * 由上面的分析，如当我们调用 shutdownNow 时：
                  	 * 如果在这里线程正在执行可响应中断的方法，如 sleep 等时，这里会抛出 InterruptedException 异常
                  	 * 如果是正常执行，则不受影响，继续执行 task 任务
                  	 */
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
      	processWorkerExit(w, completedAbruptly);
    }
}

private Runnable getTask() {
  boolean timedOut = false; // Did the last poll() time out?
  for (;;) {
      int c = ctl.get();
      int rs = runStateOf(c);

      /**
       * shutdownNow() 方法被调用后，线程池的状态被替换为 STOP，并且队列中的元素也被移除了
       * 符合判断条件，返回 null
       */
      if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
          decrementWorkerCount();
          return null;
      }

      int wc = workerCountOf(c);

      // Are workers subject to culling?
      boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

      if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
          if (compareAndDecrementWorkerCount(c))
              return null;
          continue;
      }

    	/**
    	 * 从队列中取任务，可以响应中断，但中断的处理并不能退出这个 for 循环
    	 */
      try {
        	// 这里的 poll 和 take 方法，都可以响应中断
          Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
          if (r != null)
              return r;
          timedOut = true;
      } catch (InterruptedException retry) {
          timedOut = false;
      }
    }
}
```

通过上面的分析，`getTask()`方法是线程退出的关键。`shutdownNow()` 方法里将线程修改为 STOP 状态，当执行到 `getTask()` 方法的判断时，由于 STOP 状态值是大于 SHUTDOWN 状态， STOP  也大于等于STOP，不管任务队列是否为空，都会进入 if 语句从而返回 null，线程退出。

**shutdownNow()方法总结：**

- 线程池拒绝新任务的提交
- 线程池中的线程会被中断
- 线程池中已经提交的还没有执行的任务会被清除
- 线程池等待正在执行中的任务执行完毕后，关闭线程池

##### shutdown 方法

先看下源码

```java
/**
 * 与 shutdownNow 方法最直接的不同是，showdown 不会移除队列中的任务。
 * 这就导致 runWorker 中 getTask 方法只能等队列中任务被消耗完才能返回 null
 */
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
      	// 将线程状态替换为 shutdown
        advanceRunState(SHUTDOWN);
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
       mainLock.unlock();
    }
    tryTerminate();
}

private void interruptIdleWorkers() {
  	interruptIdleWorkers(false);
}

private void interruptIdleWorkers(boolean onlyOne) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        for (Worker w : workers) {
          Thread t = w.thread;
          /**
           * w.tryLock() 方法导致了与 shutdownNow 处理方式的不同
           * runWorker 方法中 while 循环的第一步就是获取 woker 的锁
           * 这里 tryLock 尝试获取 worker 锁，会与运行中 worker 冲突，导致获取不到锁，也就无法调用正在运行的的线程的interrupt 方法，该线程也就不会被中断
           */
          if (!t.isInterrupted() && w.tryLock()) {
              try {
                  t.interrupt();
              } catch (SecurityException ignore) {
              } finally {
                  w.unlock();
              }
          }
          if (onlyOne)
              break;
        }
    } finally {
        mainLock.unlock();
    }
}
```

**shutdown()方法总结：**

- 线程池拒绝新任务的提交
- 如果线程正在执行线程池里的任务，即便任务处于阻塞状态，线程也不会被中断，而是继续执行
- 线程池将所有已经被提交的任务执行完毕后关闭线程池

### 12.5 线程池中的其他有用方法

大家可能有留意到，我在创建线程池的时候，还调用了这个方法：**prestartAllCoreThreads**。这个方法有什么作用呢？我们知道一个线程池创建出来之后，在没有给它提交任何任务之前，这个线程池中的线程数为0。有时候我们事先知道会有很多任务会提交给这个线程池，但是等它一个个去创建新线程开销太大，影响系统性能，因此可以考虑在创建线程池的时候就将所有的核心线程全部一次性创建完毕，这样系统起来之后就可以直接使用了。

其实线程池中还提供了其他一些比较有意思的方法。比如我们现在设想一个场景，当一个线程池负载很高，快要撑爆导致触发拒绝策略时，有没有什么办法可以缓解这一问题？其实是有的，因为线程池提供了设置核心线程数和最大线程数的方法，它们分别是`setCorePoolSize`方法和`setMaximumPoolSize`方法。是的，**线程池创建完毕之后也是可以更改其线程数的！**因此，面对线程池高负荷运行的情况，我们可以这么处理：

1. 起一个定时轮询线程（守护类型），定时检测线程池中的线程数，具体来说就是调用getActiveCount方法
2. 当发现线程数超过了核心线程数大小时，可以考虑将CorePoolSize和MaximumPoolSize的数值同时乘以2，当然这里不建议设置很大的线程数，因为并不是线程越多越好的，可以考虑设置一个上限值，比如50, 100之类的。
3. 同时，去获取队列中的任务数，具体来说是调用getQueue方法再调用size方法。当队列中的任务数少于队列大小的二分之一时，我们可以认为现在线程池的负载没有那么高了，因此可以考虑在线程池先前有扩容过的情况下，将CorePoolSize和MaximumPoolSize还原回去，也就是除以2