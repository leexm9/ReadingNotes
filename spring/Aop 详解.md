## AOP 详解

### 1 AOP 中涉及到几个关键概念：

- `Advice：`通知，定义在连接点做什么，比如我们在方法前后进行日志打印（前置通知、后置通知、环绕通知等等）
- `Pointcut：`切点，决定advice应该作用于那个连接点，比如根据**正则等规则匹配**哪些方法需要增强（Pointcut 目前有getClassFilter（类匹配），getMethodMatcher（方法匹配），Pointcut.TRUE （全匹配））
- `JoinPoint`：连接点，就是spring允许你是通知（Advice）的地方，那可就真多了，基本每个方法的前、后（两者都有也行），或抛出异常时都可以是连接点，**spring只支持方法连接点**。其他如AspectJ还可以让你在构造器或属性注入时都行，不过那不是咱们关注的，只要记住，和方法有关的前前后后都是连接点（**通知方法里都可以获取到这个连接点，顺便获取到相关信息**）。
-  `Advisor`：把pointcut和advice连接起来（可由Spring去完成，我们都交给容器管理就行，当然，你也可以手动完成）Spring的Advisor是Pointcut和Advice的配置器，它是将Advice注入程序中Pointcut位置的代码。`org.springframework.aop.support.DefaultPointcutAdvisor`是最通用的Advisor类

### 2 几个重要的类、接口详解

#### 2.1 ProxyConfig

Aop 配置类，用于创建代理的配置的**父类**，以确保所有代理创建者具有**一致的属性**。 它有五个属性，解释如下：

```java
public class ProxyConfig implements Serializable {
    /**
     * 标记是否直接对目标类进行代理，而不是通过接口产生代理
     */
    private boolean proxyTargetClass = false;
    
  	/**
  	 * 标记是否对代理进行优化。true：那么在生成代理对象之后，如果对代理配置进行了修改，已经创建的代理对象也不会获取修改之后的代理配置。
     * 如果exposeProxy设置为true，即使optimize为true也会被忽略。
     */
    private boolean optimize = false;
  
  	/**
     * 标记是否需要阻止通过该配置创建的代理对象转换为Advised类型，默认值为false，表示代理对象可以被转换为Advised类型
     * Advised接口其实就代表了被代理的对象（此接口是Spring AOP提供，它提供了方法可以对代理进行操作，比如移除一个切面之类的），它持有了代理对象的一些属性，通过它可以对生成的代理对象的一些属性进行人为干预
     * 默认情况，我们可以这么玩 Advised target = (Advised) context.getBean("opaqueTest"); 从而就可以对该代理持有的一些属性进行干预勒；若此值为true，就不能这么玩了
     */
    boolean opaque = false;
  
  	/**
     * 标记代理对象是否应该被aop框架通过AopContext以ThreadLocal的形式暴露出去。
     * 当一个代理对象需要调用它【自己】的另外一个代理方法时，这个属性将非常有用。默认是是false，以避免不必要的拦截。
     */
    boolean exposeProxy = false;
  
  	/**
     * 标记是否需要冻结代理对象，即在代理对象生成之后，是否允许对其进行修改，默认为false.
     * 当我们不希望调用方修改转换成Advised对象之后的代理对象时，就可以设置为true 给冻结上即可
     */
    private boolean frozen = false;
}
```

#### 2.2 ProxyProcessorSupport

简单的说它就是提供为代理创建器提供了一些公共方法实现

```java
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {
  
  /**
	 * AOP的自动代理创建器必须在所有的别的processors之后执行，以确保它可以代理到所有的小伙伴们，即使需要双重代理得那种
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;


	/**
	 * 当然此处还是提供了方法，你可以自己set或者使用@Order来人为的改变这个顺序
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the containing
	 * {@link org.springframework.beans.factory.BeanFactory} for loading all bean classes.
	 * This can be overridden here for specific proxies.
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	/**
	 * Return the configured proxy ClassLoader for this processor.
	 */
	@Nullable
	protected ClassLoader getProxyClassLoader() {
		return this.proxyClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}


	/**
	 * Check the interfaces on the given bean class and apply them to the {@link ProxyFactory},
	 * if appropriate.
	 * <p>Calls {@link #isConfigurationCallbackInterface} and {@link #isInternalLanguageInterface}
	 * to filter for reasonable proxy interfaces, falling back to a target-class proxy otherwise.
	 * @param beanClass the class of the bean
	 * @param proxyFactory the ProxyFactory for the bean
	 */
	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
		boolean hasReasonableProxyInterface = false;
		for (Class<?> ifc : targetInterfaces) {
			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
					ifc.getMethods().length > 0) {
				hasReasonableProxyInterface = true;
				break;
			}
		}
		if (hasReasonableProxyInterface) {
			// Must allow for introductions; can't just set interfaces to the target's interfaces only.
			for (Class<?> ifc : targetInterfaces) {
				proxyFactory.addInterface(ifc);
			}
		}
    // 这个很明显设置true，表示使用CGLIB得方式去创建代理了
		else {
			proxyFactory.setProxyTargetClass(true);
		}
	}

	/**
	 * Determine whether the given interface is just a container callback and
	 * therefore not to be considered as a reasonable proxy interface.
	 * <p>If no reasonable proxy interface is found for a given bean, it will get
	 * proxied with its full target class, assuming that as the user's intention.
	 * @param ifc the interface to check
	 * @return whether the given interface is just a container callback
	 * 判断此接口类型是否属于：容器去回调的类型，这里例举处理一些接口 初始化、销毁、自动刷新、自动关闭、Aware感知等等
	 */
	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
		return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
				AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
	}

	/**
	 * Determine whether the given interface is a well-known internal language interface
	 * and therefore not to be considered as a reasonable proxy interface.
	 * <p>If no reasonable proxy interface is found for a given bean, it will get
	 * proxied with its full target class, assuming that as the user's intention.
	 * @param ifc the interface to check
	 * @return whether the given interface is an internal language interface
	 * 是否是如下通用的接口。若实现的是这些接口也会排除，不认为它是实现了接口的类
	 */
	protected boolean isInternalLanguageInterface(Class<?> ifc) {
		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
				ifc.getName().endsWith(".cglib.proxy.Factory") ||
				ifc.getName().endsWith(".bytebuddy.MockAccess"));
	}

}
```

#### 2. 3 ProxyCreatorSupport

主要用于设置和保存以下三大信息：

- 设置被代理对象target
- 设置代理接口
- 设置通知advice

```java
public class ProxyCreatorSupport extends AdvisedSupport {
	
	// new了一个aopProxyFactory 
	public ProxyCreatorSupport() {
		this.aopProxyFactory = new DefaultAopProxyFactory();
	}
	
  /**
   * createAopProxy 是入口方法
   */
	protected final synchronized AopProxy createAopProxy() {
		if (!this.active) {
			activate();
		}
		/**
     * 由此可议看出，它还是委托给了`AopProxyFactory`去做这件事 
     * 它的实现类为：DefaultAopProxyFactory
     */
		return getAopProxyFactory().createAopProxy(this);
	}
}

//DefaultAopProxyFactory#createAopProxy
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		// 对代理进行优化 或者 直接采用CGLIB动态代理  或者 
		//config.isOptimize()与config.isProxyTargetClass()默认返回都是false
		// 需要优化  强制cglib  没有实现接口等都会进入这里面来
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 倘若目标Class本身就是个接口，或者它已经是个JDK的代理类（Proxy的子类。所有的JDK代理类都是此类的子类）
      // 那还是用JDK的动态代理吧
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// 使用 CGLIB代理方式 ObjenesisCglibAopProxy是CglibAopProxy的子类。Spring4.0之后提供的
			return new ObjenesisCglibAopProxy(config);
		}
		// 否则（一般都是有实现接口） 都会采用JDK得动态代理
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	// 如果它没有实现过接口（ifcs.length == ）或者 仅仅实现了一个接口，但是这个接口却是SpringProxy类型的，那就返回false
	// 总体来说，就是看看这个cofnig有没有实现过靠谱的、可以用的接口
	// SpringProxy:一个标记接口。Spring AOP产生的所有的代理类 都是它的子类
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}
}
```

#### 2.4 Advised 和 AdvisedSupport
`Advised` 包含所有的`Advisor` 和 `Advice`

```java
public interface Advised extends TargetClassAware {
    boolean isFrozen();
    boolean isProxyTargetClass();

    // 返回被代理了的接口
    Class<?>[] getProxiedInterfaces();
    // 检查这个指定的接口是否被代理了
    boolean isInterfaceProxied(Class<?> intf);
  
    // 设置一个源。只有isFrozen为false才能调用此方法
    void setTargetSource(TargetSource targetSource);
    TargetSource getTargetSource();

    void setExposeProxy(boolean exposeProxy);
    boolean isExposeProxy();

    // 默认是false，和ClassFilter接口有关，暂时不做讨论
    void setPreFiltered(boolean preFiltered);
    boolean isPreFiltered();

    // 拿到作用在当前代理上的所有通知（和切面的适配器）
    Advisor[] getAdvisors();

    // 相当于在通知（拦截器）链的最后一个加入一个新的
    void addAdvisor(Advisor advisor) throws AopConfigException;
    void addAdvisor(int pos, Advisor advisor) throws AopConfigException;
    boolean removeAdvisor(Advisor advisor);
    // 按照下标移除一个通知
    void removeAdvisor(int index) throws AopConfigException;
    int indexOf(Advisor advisor);
    boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

    // 增加通知得相关方法，采用了适配器的模式，最终都会变成一个DefaultIntroductionAdvisor(包装Advice的)
    void addAdvice(Advice advice) throws AopConfigException;
    void addAdvice(int pos, Advice advice) throws AopConfigException;
    boolean removeAdvice(Advice advice);
    int indexOf(Advice advice);

    String toProxyConfigString();
}
```

```java
public class AdvisedSupport extends ProxyConfig implements Advised {
    @Override
    public void addAdvisor(Advisor advisor) {
      int pos = this.advisors.size();
      addAdvisor(pos, advisor);
    }
    @Override
    public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
      if (advisor instanceof IntroductionAdvisor) {
        validateIntroductionAdvisor((IntroductionAdvisor) advisor);
      }
      addAdvisorInternal(pos, advisor);
    }	

    // advice最终都会备转换成一个`Advisor`（DefaultPointcutAdvisor  表示切面+通知），它使用的切面为Pointcut.TRUE
    // Pointcut.TRUE：表示啥都返回true，也就是说这个增强通知将作用于所有的方法上/所有的方法
    // 若要自己指定切面（比如切点表达式）,使用它的另一个构造函数：public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice)
    @Override
    public void addAdvice(Advice advice) throws AopConfigException {
      int pos = this.advisors.size();
      addAdvice(pos, advice);
    }
    @Override
    public void addAdvice(int pos, Advice advice) throws AopConfigException {
      Assert.notNull(advice, "Advice must not be null");
      if (advice instanceof IntroductionInfo) {
        // We don't need an IntroductionAdvisor for this kind of introduction:
        // It's fully self-describing.
        addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
      }
      else if (advice instanceof DynamicIntroductionAdvice) {
        // We need an IntroductionAdvisor for this kind of introduction.
        throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
      }
      else {
        addAdvisor(pos, new DefaultPointcutAdvisor(advice));
      }
    }

    // 这里需要注意的是：setTarget最终的效果其实也是转换成了TargetSource
    // 也就是说Spring最终代理的  是放进去TargetSource让它去处理
    public void setTarget(Object target) {
      setTargetSource(new SingletonTargetSource(target));
    }
    @Override
    public void setTargetSource(@Nullable TargetSource targetSource) {
      this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
    }


    //... 其它实现略过，基本都是实现Advised接口的内容

    //将之前注入到advisorChain中的advisors转换为MethodInterceptor和InterceptorAndDynamicMethodMatcher集合（放置了这两种类型的数据）
    // 这些MethodInterceptor们最终在执行目标方法的时候都是会执行的
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
      // 以这个Method生成一个key，准备缓存 
      // 此处小技巧：当你的key比较复杂时，可以用类来处理。然后重写它的equals、hashCode、toString、compare等方法
      MethodCacheKey cacheKey = new MethodCacheKey(method);
      List<Object> cached = this.methodCache.get(cacheKey);
      if (cached == null) {
        // 这个方法最终在这 DefaultAdvisorChainFactory#getInterceptorsAndDynamicInterceptionAdvice
        // DefaultAdvisorChainFactory：生成通知器链的工厂，实现了interceptor链的获取过程
        cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
            this, method, targetClass);

        // 此处为了提供效率，相当于把该方法对应的拦截器们都缓存起来，加速后续调用得速度
        this.methodCache.put(cacheKey, cached);
      }
      return cached;
    }
}

//DefaultAdvisorChainFactory：生成拦截器链
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

    @Override
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
        Advised config, Method method, @Nullable Class<?> targetClass) {

      // This is somewhat tricky... We have to process introductions first,
      // but we need to preserve order in the ultimate list.
      AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
      Advisor[] advisors = config.getAdvisors();
      List<Object> interceptorList = new ArrayList<>(advisors.length);
      Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
      Boolean hasIntroductions = null;

      for (Advisor advisor : advisors) {
        if (advisor instanceof PointcutAdvisor) {
          // Add it conditionally.
          PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
          if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
            MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
            boolean match;
            if (mm instanceof IntroductionAwareMethodMatcher) {
              if (hasIntroductions == null) {
                hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
              }
              match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
            }
            else {
              match = mm.matches(method, actualClass);
            }
            if (match) {
              MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
              if (mm.isRuntime()) {
                // Creating a new object instance in the getInterceptors() method
                // isn't a problem as we normally cache created chains.
                for (MethodInterceptor interceptor : interceptors) {
                  interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
                }
              }
              else {
                interceptorList.addAll(Arrays.asList(interceptors));
              }
            }
          }
        }
        else if (advisor instanceof IntroductionAdvisor) {
          IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
          if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
            Interceptor[] interceptors = registry.getInterceptors(advisor);
            interceptorList.addAll(Arrays.asList(interceptors));
          }
        }
        else {
          Interceptor[] interceptors = registry.getInterceptors(advisor);
          interceptorList.addAll(Arrays.asList(interceptors));
        }
      }

      return interceptorList;
    }

    /**
     * Determine whether the Advisors contain matching introductions.
     */
    private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
      for (Advisor advisor : advisors) {
        if (advisor instanceof IntroductionAdvisor) {
          IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
          if (ia.getClassFilter().matches(actualClass)) {
            return true;
          }
        }
      }
      return false;
    }

}
```

#### 2.5 AdvisorAdapter

spring aop框架对`BeforeAdvice、AfterAdvice、ThrowsAdvice`三种通知类型的支持实际上是借助**适配器模式**来实现的，这样的好处是使得框架允许用户向框架中加入自己想要支持的任何一种通知类型。

`AdvisorAdapter`是一个适配器接口，它定义了自己支持的`Advice`类型，并且能把一个`Advisor`适配成`MethodInterceptor`（这也是AOP联盟定义的借口），以下是它的定义

```java
public interface AdvisorAdapter {
    // 判断此适配器是否支持特定的Advice  
    boolean supportsAdvice(Advice advice);  
    // 将一个Advisor适配成MethodInterceptor  
    MethodInterceptor getInterceptor(Advisor advisor);  
}

// 一般我们自己并不需要自己去提供此接口的实现（除非你还行适配被的Advice进来），因为Spring为我们提供了对应的实现：
public class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {
	@Override
	public boolean supportsAdvice(Advice advice) {
		return (advice instanceof MethodBeforeAdvice);
	}

	@Override
	public MethodInterceptor getInterceptor(Advisor advisor) {
		MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
		return new MethodBeforeAdviceInterceptor(advice);
	}
}

// 都转为了AOP联盟的MethodInterceptor 从而实现拦截统一的拦截工作
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, Serializable {
	private MethodBeforeAdvice advice;
  
	/**
	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
	 * @param advice the MethodBeforeAdvice to wrap
	 */
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
		// 最终调用，实现了链式调用的效果
		return mi.proceed();
	}
}
```

参考：`AdvisorAdapterRegistry`、`DefaultAdvisorAdapterRegistry`和`GlobalAdvisorAdapterRegistry`用于管理管理`AdvisorAdapter`的。

如果我们想把自己定义的`AdvisorAdapter`注册到`spring aop`框架中：

- 把我们自己写好得`AdvisorAdapter`放进`Spring IoC`容器中
- 配置一个`AdvisorAdapterRegistrationManager`，它是一个`BeanPostProcessor`，它会检测所有的`Bean`。若是`AdvisorAdapter`类型，就：`this.advisorAdapterRegistry.registerAdvisorAdapter((AdvisorAdapter) bean)`;

#### 2.6 TargetSource

该接口代表一个目标对象，在aop调用目标对象的时候，使用该接口返回真实的对象。
比如它有其中两个实现SingletonTargetSource和PrototypeTargetSource代表着每次调用返回同一个实例，和每次调用返回一个新的实例

#### 2.7 TargetClassAware

所有的Aop代理对象或者代理工厂（proxy factory)都要实现的接口，该接口用于暴露出被代理目标对象类型；

#### 2.8 AspectMetadata

表示一个切面的元数据类

```java
public class AspectMetadata implements Serializable {
		private final String aspectName;
		private final Class<?> aspectClass;
		// AjType这个字段非常的关键，它表示有非常非常多得关于这个切面的一些数据、方法（位于org.aspectj下）
		private transient AjType<?> ajType;
	
		// 解析切入点表达式用的，但是真正的解析工作为委托给 org.aspectj.weaver.tools.PointcutExpression 来解析的
		// 若是单例：则是Pointcut.TRUE，否则为AspectJExpressionPointcut
		private final Pointcut perClausePointcut;

		public AspectMetadata(Class<?> aspectClass, String aspectName) {
				this.aspectName = aspectName;

				Class<?> currClass = aspectClass;
				AjType<?> ajType = null;
		
				// 此处会一直遍历到顶层直到 Object，直到找到有一个是Aspect切面就行，然后保存起来
				// 因此我们的切面写在父类上也是可以的
				while (currClass != Object.class) {
						AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
						if (ajTypeToCheck.isAspect()) {
								ajType = ajTypeToCheck;
								break;
						}
						currClass = currClass.getSuperclass();
				}
		
				// 由此可见，我们传进来的Class必须是个切面或者切面的子类的
        if (ajType == null) {
        		throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
        }
      
				// 显然Spring AOP目前也不支持优先级的声明
				if (ajType.getDeclarePrecedence().length > 0) {
						throw new IllegalArgumentException("DeclarePrecendence not presently supported in Spring AOP");
				}
      
				this.aspectClass = ajType.getJavaClass();
				this.ajType = ajType;

				// 切面的处的类型：PerClauseKind，由此可议看出，Spring的AOP目前只支持下面4种 
        switch (this.ajType.getPerClause().getKind()) {
            case SINGLETON:
              // 如果是单例，这个表达式返回这个常量
              this.perClausePointcut = Pointcut.TRUE;
              return;
            case PERTARGET:
            case PERTHIS:
              // PERTARGET 和 PERTHIS 处理方式一样，返回的是AspectJExpressionPointcut
              AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
              ajexp.setLocation(aspectClass.getName());
              // 设置好切点表达式
              ajexp.setExpression(findPerClause(aspectClass));
              ajexp.setPointcutDeclarationScope(aspectClass);
              this.perClausePointcut = ajexp;
              return;
            case PERTYPEWITHIN:
              // 组成的、合成的切点表达式
              this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
              return;
            default:
              // 其余的Spring AOP暂时不支持
              throw new AopConfigException(
                "PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
        }
	}

    private String findPerClause(Class<?> aspectClass) {
        String str = aspectClass.getAnnotation(Aspect.class).value();
        str = str.substring(str.indexOf('(') + 1);
        str = str.substring(0, str.length() - 1);
        return str;
    }
	
  	// ......
  
    public Pointcut getPerClausePointcut() {
      	return this.perClausePointcut;
    }
  
    // 判断 perThis 或者 perTarget 类型，最单实例、多实例处理
    public boolean isPerThisOrPerTarget() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
    }
  
    // 是否是 within 的类型
    public boolean isPerTypeWithin() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTYPEWITHIN);
    }
  
    // 只要不是单例的，就都属于懒加载，延迟实例化的类型
    public boolean isLazilyInstantiated() {
        return (isPerThisOrPerTarget() || isPerTypeWithin());
    }
}
```

Spring AOP支持AspectJ的singleton、perthis、pertarget、pertypewithin实例化模型（目前不支持percflow、percflowbelow）， 参见枚举类PerClauseKind：

- singleton：即切面只会有一个实例;

  使用`@Aspect()`指定

- perthis：每个切入点表达式匹配的连接点对应的AOP对象（代理对象）都会创建一个新切面实例；

  使用`@Aspect("perthis(切入点表达式)")`指定切入点表达式

  ```java
  // 将为每个被切入点表达式匹配上的代理对象，都创建一个新的切面实例（此处允许HelloService是接口）@Aspect("perthis(com.fsx.HelloService)")
  ```

- pertarget：每个切入点表达式匹配的连接点对应的目标对象都会创建一个新的切面实例

  使用`@Aspect("pertarget(切入点表达式)")`指定切入点表达式

- pertypewithin

> 默认是singleton实例化模型，Schema风格只支持singleton实例化模型，而@AspectJ风格支持这三种实例化模型

#### 2.9 AspectInstanceFactory

切面工厂，专门为切面创建实例的工厂（因为切面也不一定是单例的，也支持各种多例形式）

```java
// 它实现了Order接口哦~~~~支持排序的
public interface AspectInstanceFactory extends Ordered {
    // Create an instance of this factory's aspect.
    Object getAspectInstance();
    // Expose the aspect class loader that this factory uses.
    @Nullable
    ClassLoader getAspectClassLoader();
}
```

它的具体实现类有：

- `SimpleAspectInstanceFactory`：根据切面的aspectClass，调用空构造函数反射.newInstance()创建一个实例（备注：构造函数private的也没有关系）

- `SingletonAspectInstanceFactory`：这个就更简单了，因为已经持有aspectInstance的引用了，直接return即可

- `MetadataAwareAspectInstanceFactory`：`AspectInstanceFactory`的子接口，提供了获取AspectMetadata的方法

  ```java
  public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {
      AspectMetadata getAspectMetadata();
      // Spring4.3提供  和beanFactory.getSingletonMutex()，否则一般都是this
      Object getAspectCreationMutex();
  }	
  ```

- `BeanFactoryAspectInstanceFactory`：这个就和Bean工厂有关了，比较重要

  ```java
  public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {
      // 持有对Bean工厂的引用
      private final BeanFactory beanFactory;
    
      // 需要处理的名称
      private final String name;
      private final AspectMetadata aspectMetadata;
  
      // 传了Name，type可议不传，内部判断出来
      public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
        	this(beanFactory, name, null);
      }
    
      public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, 
                                              @Nullable Class<?> type) {
          this.beanFactory = beanFactory;
          this.name = name;
          Class<?> resolvedType = type;
          // 若没传type，就去Bean工厂里看看它的Type是啥，type不能为null
          if (type == null) {
            	resolvedType = beanFactory.getType(name);
            	Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
          }
          // 包装成切面元数据类
          this.aspectMetadata = new AspectMetadata(resolvedType, name);
      }
  
      // 此处切面实例是从Bean工厂里获取的，需要注意若是：多例的，请注意Scope的值
      @Override
      public Object getAspectInstance() {
        	return this.beanFactory.getBean(this.name);
      }
  
      @Override
      @Nullable
      public ClassLoader getAspectClassLoader() {
          return (this.beanFactory instanceof ConfigurableBeanFactory ?
              ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
              ClassUtils.getDefaultClassLoader());
      }
  
      @Override
      public AspectMetadata getAspectMetadata() {
       	 return this.aspectMetadata;
      }
  
      @Override
      @Nullable
      public Object getAspectCreationMutex() {
          if (this.beanFactory.isSingleton(this.name)) {
            // Rely on singleton semantics provided by the factory -> no local lock.
            return null;
          }
          else if (this.beanFactory instanceof ConfigurableBeanFactory) {
            // No singleton guarantees from the factory -> let's lock locally but
            // reuse the factory's singleton lock, just in case a lazy dependency
            // of our advice bean happens to trigger the singleton lock implicitly...
            return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
          }
          else {
            return this;
          }
      }
  
      @Override
      public int getOrder() {
          Class<?> type = this.beanFactory.getType(this.name);
          if (type != null) {
            if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
              return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
            }
            // 若没实现接口，就拿注解的值
            return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
          }
          return Ordered.LOWEST_PRECEDENCE;
      }
  }
  ```

- `PrototypeAspectInstanceFactory`：多例专用的工厂， 若是多例的，则推荐使用

  ```java
  public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements 				Serializable {
      public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
          super(beanFactory, name);
          // 若是单例，直接报错了
          if (!beanFactory.isPrototype(name)) {
              throw new IllegalArgumentException(
                  "Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
          }
      }
  }
  ```

### 3 具体创建过程


