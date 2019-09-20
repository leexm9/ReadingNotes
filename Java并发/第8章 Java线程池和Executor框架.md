## 第8章 Java 线程池和Executor框架

[TOC]

### 8.1 线程池的实现原理

在开发过程中，合理地使用线程可以带来以下的好处：

- 降低资源消耗：通过重复利用已创建的线程降低线程创建和销毁造成的消耗；
- 提高响应速度：当任务到达时，任务可以不需要等到线程创建就能立即执行；
- 提高线程的可管理性：线程是稀缺资源，如果无限制地创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池可以进行统一分配、调优和监控。

**线程池的处理流程：**

![线程池主要工作流程](./pictures/threadPool1.png)

**ThreadPoolExecutor 执行 execute() 方法示意图：**

![execute方法示意图](./pictures/threadPool2.png)

1. 如果当前运行的线程少于 *corePoolSize*，则创建新线程来执行任务（注意，执行这一步需要获取全局的锁）；
2. 如果运行的线程等于或多于 *corePoolSize*，则将任务加入 *BlockingQueue*；
3. 如果无法将任务加入到 *BlockingQueue*（队列已满），则创建新的线程来处理任务（注意，执行这一步需要获取全局的锁）；
4. 如果创建新线程将使当前运行的线程超出 *maximumPoolSize*，任务将被拒绝，并调用 *RejectedExecutionHandler.rejectedExecution()* 方法。

*ThreadPoolExecutor* 采取上述步骤的总体设计思路，是为了在执行 *execute()* 方法时，尽可能地避免获取全局锁（那将会是一个严重的可伸缩瓶颈）。在 *ThreadPoolExecutor* 完成预热之后（当前运行的线程数大于等于 *corePoolSize*），几乎所有的 *execute()* 方法调用都是执行*步骤2*，而*步骤2*不需要获取全局锁。

### 8.2 线程池的使用

#### 8.2.1 线程池创建

```java
/**
 * 线程池构造方法之一
 */
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                          RejectedExecutionHandler handler){
    .....
}
```

创建线程池时的参数：

- *corePoolSize*：基本线程数，当提交一个任务到线程池时，线程池会创建一个线程来执行任务，即使其他空闲的基本线程能够执行任务也会创建线程，等到需要执行的任务数大于线程池基本大小时就不再创建了。如果调用了线程池的 *prestartAllCoreThreads()* 方法，线程池会提前创建并启动所有基本线程。

- *maximumPoolSize*：线程池最大数量，线程池允许创建的最大线程数。如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。注意：如果使用了无界队列，这个参数就没有效果了。

- *BlockingQueue*：任务队列，用于保存等待执行的任务的阻塞队列

- *ThreadFactory*：用于设置创建线程的工厂类，可以通过线程工厂给每个创建出来的线程设置更有意义的名字。

- *RejectedExecutionHandler*：饱和策略，当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。这个策略默认情况下是 *AbortPolicy*，表示无法处理新任务时抛出异常。

  - AbortPolicy：直接抛出异常

  - CallerRunsPolicy：只用调用者所在的线程来运行任务

  - DiscardOldestPolicy：丢弃队列里最近的一个任务，并执行当前任务

  - DiscardPolicy：不处理，丢弃掉

    当然，也可以根据应用场景需要来实现 *RejectedExecutionHandler* 接口自定义策略，如记录日志或持久化存储不能处理的任务

- keepAliveTime：线程活动保持时间，线程池的工作线程空闲后，保持存活的时间，用于非核心线程。如果任务很多，并且每个任务执行的时间比较短，可以调大时间，提高线程的利用率。

- TimeUnit：线程活动时间保持单位

#### 8.2.2 向线程池提交任务

- *execute* 提交的任务，但是 *execute* 方法没有返回值，所以无法判断任务知否被线程池执行成功。

  ```java
  threadsPool.execute(new Runnable() {
      @Override
      public void run() {
          .....
      }
  })
  ```

-  *submit* 方法来提交任务，它会返回一个 **future**， 那么我们可以通过这个 **future** 来判断任务是否执行成功，通过 **future** 的 *get* 方法来获取返回值，*get* 方法会阻塞住直到任务完成，而使用 *get(long timeout, TimeUnit unit)* 方法则会阻塞一段时间后立即返回，这时有可能任务没有执行完。

  ```java
  Future<Object> future = executor.submit(returnValueTask);
  try {
      Object a = future.get();
  } catch (InterruptedException e) {
      // 处理中断异常
  } catch (ExecutionException e) {
      // 无法执行任务异常
  } finally {
      // 关闭线程池
      executor.shutdown();
  }
  ```


#### 8.2.3 关闭线程池

以通过调用线程池的 *shutdown()* 或 *shutdownNow()* 方法来关闭线程池，但是它们的实现原理不同：

- *shutdown()*：是将线程池的状态设置成 **SHUTDOWN** 状态，然后中断所有没有正在执行任务的线程。
- *shutdownNow()*：是遍历线程池中的工作线程，然后逐个调用线程的 *interrupt()* 方法来中断线程，所以无法响应中断的任务可能永远无法终止。*shutdownNow()* 会首先将线程池的状态设置成 **STOP**，然后尝试停止所有的正在执行或暂停任务的线程，并返回等待执行任务的列表。

只要调用了这两个关闭方法的其中一个，*isShutdown()* 方法就会返回 **true**。当所有的任务都已关闭后，才表示线程池关闭成功，这时调用 *isTerminaed()* 方法会返回 **true**。至于我们应该调用哪一种方法来关闭线程池，应该由提交到线程池的任务特性决定，通常调用 *shutdown()* 来关闭线程池，如果任务不一定要执行完，则可以调用 *shutdownNow()*。

#### 8.2.4 合理的配置线程池

要想合理的配置线程池，就必须首先分析任务特性，可以从以下几个角度来进行分析：

1. 任务的性质：CPU密集型任务，IO密集型任务和混合型任务；
2. 任务的优先级：高，中和低；
3. 任务的执行时间：长，中和短；
4. 任务的依赖性：是否依赖其他系统资源，如数据库连接；

任务性质不同的任务可以用不同规模的线程池分开处理。

- CPU密集型任务配置尽可能少的线程数量，如配置 **Ncpu+1** 个线程的线程池。
- IO密集型任务则由于需要等待IO操作，线程并不是一直在执行任务，则配置尽可能多的线程，如 **2*Ncpu**。
- 混合型的任务，如果可以拆分，则将其拆分成一个CPU密集型任务和一个IO密集型任务，只要这两个任务执行的时间相差不是太大，那么分解后执行的吞吐率要高于串行执行的吞吐率，如果这两个任务执行时间相差太大，则没必要进行分解。

> 我们可以通过Runtime.getRuntime().availableProcessors()方法获得当前设备的CPU个数。

优先级不同的任务可以使用优先级队列 *PriorityBlockingQueue* 来处理。它可以让优先级高的任务先得到执行，需要注意的是如果一直有优先级高的任务提交到队列里，那么优先级低的任务可能永远不能执行。

执行时间不同的任务可以交给不同规模的线程池来处理，或者也可以使用优先级队列，让执行时间短的任务先执行。

依赖数据库连接池的任务，因为线程提交SQL后需要等待数据库返回结果，如果等待的时间越长CPU空闲时间就越长，那么线程数应该设置越大，这样才能更好的利用CPU。

#### 8.2.4 线程池的监控

线程池里有一些属性在监控线程池的时候可以使用

- *taskCount*：线程池需要执行的任务数量；
- *completedTaskCount*：线程池在运行过程中已完成的任务数量，小于等于 *taskCount*；
- *largestPoolSize*：线程池里曾经创建的最大线程数量。通过这个数据可以知道线程池是否曾经满过。如果该数值等于线程池的最大大小，则表示线程池曾经满过
- *getPoolSize*：线程池的线程数量。如果线程池不销毁的话，线程池里的线程不会自动销毁，所以这个数据只增不减
- *getActiveCount*：获取活动的线程数

通过扩展线程池进行监控。通过继承线程池并重写线程池的 *beforeExecute*，*afterExecute* 和 *terminated* 方法，可以在任务执行前，执行后和线程池关闭前干一些事情。如监控任务的平均执行时间，最大执行时间和最小执行时间等。

### 8.3 Executor 框架简介
![任务的两级调度模型](./pictures/executor1.png)

在 HotSpot VM 的线程模型中，Java 线程（java.lang.Thread）被一对一映射为本地操作系统线程。Java 线程启动时会创建一个本地操作系统线程；当该 Java 线程终止时，这个操作系统线程也会被回收。操作系统会调度所有线程并将它们分配给可用的 CPU。

在上层，Java 多线程程序通常把应用分解为若干个任务，然后使用用户级的调度器（Executor 框架）将这些任务映射到固定数量的线程；在底层，操作系统内核将这些线程映射到硬件处理器上，不受应用程序的控制。

![Executor 框架的使用示意图](./pictures/executor2.png)

- Executor 接口：它是 Executor 框架的基础，它将任务的提交与任务的执行分离开来
- ThreadPoolExecutor 是线程池的核心实现类，用来执行被提交的任务
- ScheduledThreadPoolExecutor 是一个实现类，可以在给定的延迟后运行命令，或者定期执行命令。它比Timer更灵活，功能更强大
- Future 接口和实现 Future 接口的 FutureTask 类，代表异步计算的结果
- Runnable 接口和 Callable 接口的实现类，都可以被 ThreadPoolExecutor 或 ScheduledThreadPoolExecutor 执行
