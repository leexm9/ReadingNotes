###  第 9 章 AbstractQueuedSynchronizer 详细解读

AQS 的相关说明，参见第 5 章。

 AQS 可以理解为，锁以及这把锁对应的同步队列。队列中是以 Node 为结构的等待节点，这些节点等待这把锁可用就是锁被其他线程释放。

---

***锁的状态是用 state 这个字段来控制***，和这把锁相关的

| 控件      | 说明                     |
| --------- | ------------------------ |
| state     | 锁对应的状态             |
| Node head | 这把锁的同步队列的首节点 |
| Node tail | 这把锁的同步队列的尾节点 |

- getState()：获取当前同步状态
- setState(int newState)：设置当前同步状态，这个方法在线程重入锁时使用
- compareAndSetState(int expect, int update)：使用 CAS 设置当前状态，如果设置成功，则说明当前线程获得了这边锁；如果设置失败，则说明有其他线程已经占有了这把锁，当前线程需要排队等待

---

***Node 节点的状态：***

| 状态      | 值   | 说明  |
| --------- | ---- | ----------------- |
| CANCELLED | 1    | 表明节点的线程出现超时或者被中断了；cancelled 状态的节点都需要从同步队列中移除 |
| SIGNAL    | -1   | 表明节点处于等到状态，需要被唤醒 |
| CONDITION | -2   | 表明节点在 Condition 的等待队列中。当其他线程调用了 Condition 的 signal() 方法后，该节点将会从等待队列中转移到同步队列中 |
| PROPAGATE | -3   | 表示下一次共享式同步状态获取将会无条件地被传播下去 |
| INITIAL   | 0    | 初始状态 |

同步队列的入队操作就是 `addWaiter(Node node)` 方法，相关流程查看第 5 章节。

----

***AQS 的主要方法***

| 方法                                               | 描述                                                         |
| :------------------------------------------------- | :----------------------------------------------------------- |
| void acquire(int arg)                              | 独占式获取同步状态，如果当前线程获取同步状态成功，则由该方法返回；否则，线程将会进入同步队列等待，该方法将会调用重写的 tryAcquire(int arg) 方法 |
| void acquireInterruptibly(int arg)                 | 响应中断的独占式获取同步状态，当前线程未获取同步状态而进入同步队列中，如果当前线程被中断，则该方法会抛出中断异常，并返回 |
| boolean tryAcquireNanos(int arg, long nanos)       | 在 acquireInterruptibly(int arg) 基础上增加了超时限制，如果当前线程在超时时间内没有获得到同步状态，那么将会返回 false；如果获取到，则返回 true |
| void acquireShared(int arg)                        | 共享式获取同步状态，如果当前线程未获取到同步状态，将会进入同步队列等待，与独占式获取的主要区别是在同一时刻可以有多个线程获得同步状态 |
| void acquireSharedInterruptibly(int arg)           | 与 acquireShared(int arg) 相同，该方法响应中断               |
| boolean tryAcquireSharedNanos(int arg, long nanos) | 在 acquireSharedInterruptibly(int arg) 的基础上增加了超时限制 |
| boolean release(int arg)                           | 独占式的释放同步状态，该方法会在释放同步状态之后，将同步队列中的头节点唤醒 |
| boolean releaseShared(int arg)                     | 共享式的释放同步状态                                         |
| Collection<Thread> getQueuedThreads()              | 获取等待在同步队列上的线程集合                               |

***AQS子类需要重写的方法***

|                    方法                     |                             描述                             |
| :-----------------------------------------: | :----------------------------------------------------------: |
|    protected boolean tryAcquire(int arg)    | 独占式获取同步状态，实现该方法需要查询当前状态并判断同步状态是否符合预期，然后再进行 CAS 设置同步状态 |
|    protected boolean tryRelease(int arg)    | 独占式释放同步状态，等待获取同步状态的线程将有机会获取同步状态 |
|   protected int tryAcquireShared(int arg)   | 共享式获取同步状态，返回大于等于0的值，表示获取成功；反之，获取失败 |
| protected boolean tryReleaseShared(int arg) |                      共享式释放同步状态                      |
|    protected boolean isHeldExclusively()    | 当前同步器是否在独占模式下被线程占用，一般该方法表示是否被当前线程占用 |

---

#### 9.1 独占式获取同步器锁，不响应中断

```java
public final void acquire(int arg) {
  /**
   * tryAcquire(arg) 获取同步器状态锁，这个方法需要子类中实现
   * addWaiter(Node.EXCLUSIVE) 方法，构造节点 node 并加入到同步队列中
   */
  if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
    selfInterrupt();
}

final boolean acquireQueued(final Node node, int arg) {
  boolean failed = true;
  try {
    boolean interrupted = false;
    // 使用自旋等待节点的前一个节点释放同步器锁
    for (;;) {
      // 节点的前一个节点
      final Node p = node.predecessor();
      // 这个地方在线程解除阻塞后，同步器锁就可以获取到了
      if (p == head && tryAcquire(arg)) {
        // 替换队列头节点，也是独占锁与共享锁差异的地方，release 方法调用一次唤醒一次头节点
        setHead(node);
        p.next = null; // help GC
        failed = false;
        return interrupted;
      }
      
      /**
       * 这个地方将同步队列中，node 节点之前的节点状态都 cas 替换为 signal
       * 线程在这个地方阻塞
       * 外部调用release()，进而调用 unparkSuccessor()方法解除阻塞，线程继续自旋
       */
      if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
        // 中断状态
        interrupted = true;
    }
  } finally {
    if (failed)
      cancelAcquire(node);
  }
}

/**
 * 判断同步队列中该节点的前继节点中是否存在 SIGNAL 状态的节点；
 * 如果有，则说明已经有线程处于等待状态，node 节点也需要排队
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
  int ws = pred.waitStatus;
  if (ws == Node.SIGNAL)
    return true;
  
  // 大于0证明，前面的线程等待超时或者已经被中断，需要从同步队列中移除
  if (ws > 0) {
    do {
      node.prev = pred = pred.prev;
    } while (pred.waitStatus > 0);
    pred.next = node;
  } else {
    // 找到小于等于0的前节点，设置为 SIGNAL
    compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
  }
  return false;
}

private final boolean parkAndCheckInterrupt() {
  // 阻塞线程
  LockSupport.park(this);
  // 到这里线程被唤醒，返回线程的中断状态并重置中断状态
  return Thread.interrupted();
}
```



#### 9.2 独占式获取同步器锁，响应中断

```java
public final void acquireInterruptibly(int arg) throws InterruptedException {
  if (Thread.interrupted())
    throw new InterruptedException();
  if (!tryAcquire(arg))
    doAcquireInterruptibly(arg);
}

private void doAcquireInterruptibly(int arg) throws InterruptedException {
  final Node node = addWaiter(Node.EXCLUSIVE);
  boolean failed = true;
  try {
    for (;;) {
      final Node p = node.predecessor();
      if (p == head && tryAcquire(arg)) {
        setHead(node);
        p.next = null; // help GC
        failed = false;
        return;
      }
      if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
        // 这里抛出了中断异常来响应中断
        throw new InterruptedException();
    }
  } finally {
    if (failed)
      cancelAcquire(node);
  }
}
```

---

#### 9.3 独占式释放同步器锁

```java
public final boolean release(int arg) {
  // 释放锁
  if (tryRelease(arg)) {
    Node h = head;
    // 头结点不为空，证明初始化了
    // 头节点的状态不是 INITIAL(0)，即证明头结点不是刚刚创建，那就可以去唤醒它的后继节点
    // 为0就证明没有其他节点了，不需要唤醒
    if (h != null && h.waitStatus != 0)
      unparkSuccessor(h);
    return true;
  }
  return false;
}

/**
 * 唤醒下一个后继节点
 */
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;
  	// 没有这个节点或者超时或者被中断了，查找一个可以用的节点
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

---

#### 9.4 借助 ***CountDownLatch*** 分析共享锁

```java
public class CountDownLatch {
  
  	/**
  	 * 内部内 Sync 继承 AQS，实现相关方法
  	 */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

      	/**
      	 * for 循环，倒数计数
      	 */
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * 需要指定倒数计数值
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }
		
  	// 等到倒数计数到达 0
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

  	// 倒数计数
    public void countDown() {
        sync.releaseShared(1);
    }

    public long getCount() {
        return sync.getCount();
    }

    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
```

使用示例：

```java
public class TestCountdownLatch {

    public static void main(String[] args) {
        ExecutorService service = Executors. newCachedThreadPool();
      	// 设置倒数计数为 1
        final CountDownLatch cdOrder = new CountDownLatch(1);
      	// 设置倒数计数为 4
        final CountDownLatch cdAnswer = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        System. out.println("选手" + Thread.currentThread().getName() + "正等待裁判发布口令");
                        // 等待倒数计数到达 0 值
                      	cdOrder.await();
                        System.out.println("选手" + Thread.currentThread().getName() + "已接受裁判口令");
                        Thread.sleep((long) (Math. random() * 10000));
                        System.out.println("选手" + Thread.currentThread().getName() + "到达终点");
                      	// 进行一次倒数计数
                        cdAnswer.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            service.execute(runnable);
        }
        try {
            Thread. sleep((long) (Math. random() * 10000));

            System. out.println("裁判" + Thread.currentThread ().getName() + "即将发布口令" );
            // 倒数计数
          	cdOrder.countDown();
            System. out.println("裁判" + Thread.currentThread ().getName() + "已发送口令，正在等待所有选手到达终点" );
          	// 等待倒数计数到达 0 值
            cdAnswer.await();
            System. out.println("所有选手都到达终点" );
            System. out.println("裁判" + Thread.currentThread ().getName() + "汇总成绩排名" );
        } catch (Exception e) {
            e.printStackTrace();
        }
        service.shutdown();
    }

}

输出：
选手pool-1-thread-1正等待裁判发布口令
选手pool-1-thread-3正等待裁判发布口令
选手pool-1-thread-4正等待裁判发布口令
选手pool-1-thread-2正等待裁判发布口令
裁判main即将发布口令
裁判main已发送口令，正在等待所有选手到达终点
选手pool-1-thread-3已接受裁判口令
选手pool-1-thread-2已接受裁判口令
选手pool-1-thread-4已接受裁判口令
选手pool-1-thread-1已接受裁判口令
选手pool-1-thread-3到达终点
选手pool-1-thread-2到达终点
选手pool-1-thread-1到达终点
选手pool-1-thread-4到达终点
所有选手都到达终点
裁判main汇总成绩排名
```



##### 9.4.1 共享式获取同步锁，响应中断

`CountDownLatch` 的 `await()` 方法，内部调用了 `AQS` 的 `acquireSharedInterruptibly` 方法

```java
public void await() throws InterruptedException {
  sync.acquireSharedInterruptibly(1);
}

/**
 * 响应中断的共享式获取同步状态
 */
public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
  if (Thread.interrupted())
    throw new InterruptedException();
  if (tryAcquireShared(arg) < 0)
    doAcquireSharedInterruptibly(arg);
}

private void doAcquireSharedInterruptibly(int arg)
  throws InterruptedException {
  final Node node = addWaiter(Node.SHARED);
  boolean failed = true;
  try {
    for (;;) {
      final Node p = node.predecessor();
      if (p == head) {
        int r = tryAcquireShared(arg);
        if (r >= 0) {
          setHeadAndPropagate(node, r);
          p.next = null; // help GC
          failed = false;
          return;
        }
      }
      if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
        throw new InterruptedException();
    }
  } finally {
    if (failed)
      cancelAcquire(node);
  }
}
```

##### 9.4.2 共享式获取同步锁，不响应中断

```java
public final void acquireShared(int arg) {
  if (tryAcquireShared(arg) < 0)
    doAcquireShared(arg);
}

private void doAcquireShared(int arg) {
  // 构造共享 Node 节点，加入同步队列
  final Node node = addWaiter(Node.SHARED);
  boolean failed = true;
  try {
    boolean interrupted = false;
    for (;;) {
      // 当前节点的前继节点
      final Node p = node.predecessor();
      // 前继节点是头节点
      if (p == head) {
        // 这个在线程解除阻塞情景下，r > 0
        int r = tryAcquireShared(arg);
        if (r >= 0) {
          // 替换头节点，唤醒后继节点，共享锁的体现，这个方法是链式反应
          setHeadAndPropagate(node, r);
          p.next = null; // help GC
          if (interrupted)
            selfInterrupt();
          failed = false;
          return;
        }
      }
      /**
       * 这个地方将同步队列中，node 节点之前的节点状态都 cas 替换为 signal
       * 线程在这个地方阻塞，当外部调用releaseShare()，进而调用 doReleaseShared()方法，解除阻塞，线程继续自旋
       */
      if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
        interrupted = true;
    }
  } finally {
    if (failed)
      cancelAcquire(node);
  }
}

/**
 * 这个方法替换同步队列中的头节点，并唤醒下一个节点
 * 共享锁之所以共享的原因，也就是这里
 */
private void setHeadAndPropagate(Node node, int propagate) {
  Node h = head;
  // 替换头节点
  setHead(node);
  if (propagate > 0 || h == null || h.waitStatus < 0 || (h = head) == null || h.waitStatus < 0) {
    Node s = node.next;
    // 当前节点没有后继节点或者后继节点是共享节点
    if (s == null || s.isShared())
      doReleaseShared();
  }
}

private void doReleaseShared() {
  for (;;) {
    Node h = head;
    if (h != null && h != tail) {
      int ws = h.waitStatus;
      // signal 状态的节点需要唤醒
      if (ws == Node.SIGNAL) {
        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
          continue;            // loop to recheck cases
        unparkSuccessor(h);
      }
      else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
        continue;                // loop on failed CAS
    }
    if (h == head)               // loop if head changed
      break;
  }
}

/**
 * 唤醒后继节点
 */
private void unparkSuccessor(Node node) {
  int ws = node.waitStatus;
  if (ws < 0)
    compareAndSetWaitStatus(node, ws, 0);
  
  Node s = node.next;
  if (s == null || s.waitStatus > 0) {
    s = null;
    for (Node t = tail; t != null && t != node; t = t.prev)
      if (t.waitStatus <= 0)
        s = t;
  }
  if (s != null)
    LockSupport.unpark(s.thread);
}
```

##### 9.4.3 release 相关

```java
public void countDown() {
  sync.releaseShared(1);
}

protected boolean tryReleaseShared(int releases) {
  // Decrement count; signal when transition to zero
  for (;;) {
    int c = getState();
    if (c == 0)
      return false;
    int nextc = c-1;
    if (compareAndSetState(c, nextc))
      return nextc == 0;
  }
}

public final boolean releaseShared(int arg) {
  if (tryReleaseShared(arg)) {
    doReleaseShared();
    return true;
  }
  return false;
}

private void doReleaseShared() {
    for (;;) {
        Node h = head;
      	// 头节点不为空也不等于尾节点，说明同步队列中有其他线程节点
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        if (h == head)                   // loop if head changed
            break;
    }
}

private void unparkSuccessor(Node node) {
  int ws = node.waitStatus;
  if (ws < 0)
    compareAndSetWaitStatus(node, ws, 0);
  
  Node s = node.next;
  if (s == null || s.waitStatus > 0) {
    s = null;
    for (Node t = tail; t != null && t != node; t = t.prev)
      if (t.waitStatus <= 0)
        s = t;
  }
  if (s != null)
    LockSupport.unpark(s.thread);
}
```

