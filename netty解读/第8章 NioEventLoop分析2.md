## 第 8 章 NioEventLoop 分析 2

在第 5 章中，我们已经知道 NioEventLoop 中的 run() 方法，执行具体的任务

```java
public final class NioEventLoop extends SingleThreadEventLoop {
  
  @Override
  protected void run() {
    for (;;) {
      try {
        try {
          switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
            case SelectStrategy.CONTINUE:
              continue;
            case SelectStrategy.BUSY_WAIT:
            case SelectStrategy.SELECT:
              select(wakenUp.getAndSet(false));
              if (wakenUp.get()) {
                selector.wakeup();
              }
            default:
          }
        } catch (IOException e) {
          rebuildSelector0();
          handleLoopException(e);
          continue;
        }

        cancelledKeys = 0;
        needsToSelectAgain = false;
        final int ioRatio = this.ioRatio;
        if (ioRatio == 100) {
          try {
            processSelectedKeys();
          } finally {
            runAllTasks();
          }
        } else {
          final long ioStartTime = System.nanoTime();
          try {
            processSelectedKeys();
          } finally {
            final long ioTime = System.nanoTime() - ioStartTime;
            runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
          }
        }
      } catch (Throwable t) {
        handleLoopException(t);
      }
      try {
        if (isShuttingDown()) {
          closeAll();
          if (confirmShutdown()) {
            return;
          }
        }
      } catch (Throwable t) {
        handleLoopException(t);
      }
    }
  }
  
}
```

我们来分析一下NioEventLoop的相关细节：

- run() 方式内部是一个无限循环，只有在遇到shutdown的情况下才会停止循环
- 然后在循环里会询问是否有事件，如果没有，则继续循环，如果有事件，那么就开始处理事件
- 前面几个章节的分析，我们已经知道 NioEventLoop 不仅要处理 I/O 事件，还要处理分 I/O 事件
- Netty 中可以设置用于I/O操作和非I/O操作的时间占比，默认各位50%，也就是说，如果某次I/O操作的时间花了100ms，那么这次循环中非I/O得任务也可以花费100ms
- Netty 中在 processSelectedKeys() 方法中处理 I/O 事件，而使用 runAllTasks() 方法处理非 I/O 事件

首先，先看看 runAllTasks() 方法

```java
protected boolean runAllTasks(long timeoutNanos) {
  // 这个方法是将定时任务队列中到期的任务放入任务队列中，供后续处理
  fetchFromScheduledTaskQueue();
  Runnable task = pollTask();
  if (task == null) {
    afterRunningAllTasks();
    return false;
  }

  final long deadline = ScheduledFutureTask.nanoTime() + timeoutNanos;
  long runTasks = 0;
  long lastExecutionTime;
  for (;;) {
    safeExecute(task);

    runTasks ++;

    // Check timeout every 64 tasks because nanoTime() is relatively expensive.
    // XXX: Hard-coded value - will make it configurable if it is really a problem.
    if ((runTasks & 0x3F) == 0) {
      lastExecutionTime = ScheduledFutureTask.nanoTime();
      if (lastExecutionTime >= deadline) {
        break;
      }
    }

    task = pollTask();
    if (task == null) {
      lastExecutionTime = ScheduledFutureTask.nanoTime();
      break;
    }
  }

  afterRunningAllTasks();
  this.lastExecutionTime = lastExecutionTime;
  return true;
}
```

可以看到，这个方法是在每运行了64个任务之后再进行比较的，如果超出了设定的运行时间则退出，否则再运行64个任务再比较。所以，Netty强烈要求不要在I/O线程中运行阻塞任务，因为阻塞任务将会阻塞住Netty的事件循环，从而造成事件堆积的现象。

然后，我们在看看 processSelectedKeys() 的处理逻辑，跟踪代码之后发现实际处理 I/O事件的方法是：

```java
private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
  final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
  if (!k.isValid()) {
    final EventLoop eventLoop;
    try {
      eventLoop = ch.eventLoop();
    } catch (Throwable ignored) {
      return;
    }
    if (eventLoop != this || eventLoop == null) {
      return;
    }
    unsafe.close(unsafe.voidPromise());
    return;
  }

  try {
    int readyOps = k.readyOps();
    // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
    // the NIO JDK channel implementation may throw a NotYetConnectedException.
    if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
      // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
      // See https://github.com/netty/netty/issues/924
      int ops = k.interestOps();
      ops &= ~SelectionKey.OP_CONNECT;
      k.interestOps(ops);

      unsafe.finishConnect();
    }

    // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
    if ((readyOps & SelectionKey.OP_WRITE) != 0) {
      // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
      ch.unsafe().forceFlush();
    }

    // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
    // to a spin loop
    if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
      unsafe.read();
    }
  } catch (CancelledKeyException ignored) {
    unsafe.close(unsafe.voidPromise());
  }
}
```

这个方法里的处理逻辑就和我们在使用原生 nio 对 channel 的连接、读、写处理基本一致了。