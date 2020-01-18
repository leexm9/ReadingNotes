## Spring 容器启动流程解读

以Spring 容器 — ApplicationContext 为例，说明容器启动过程中的各个步骤：

1. 将 xml 文件封装成 Resource，继而封装成 Document 对象

2. 采用 xml 解析工具解析 Document，将 xml 文件对象中的 Element 解析成 BeanDefinitionHolder 对象

3. 在 DefaultListableBeanFactory 中使用 Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>() 来存储由 BeanDefinitionHolder 转换而来的 BeanDefintion 对象

4. 在 xml 中不同形式的 Element 元素有不同的解析方式，对于\<XX:YY class=""...> 形式的元素采用 parseCustomElement(Element ele) 流程来解析

5. 在 parseCustomElement 解析流程中，将 Element 的解析交给各自的 NamespaceHandler 处理。这里 NamespaceHandler 的作用是将特殊 BeanDefinitionParser 初始化和实例化之后，注册进由 NamespaceHandler 管理的 Map<String, BeanDefinitionParser> parsers = new HashMap<>() 中

6. Element 具体的解析工作交由匹配的 BeanDefinitionParser 来处理，开发者可以定义自己的 NamespaceHandler 和 BeanDefinitonParser，来解析自定义的 bean

7. Aop 就是采用 NamespaceHandler 方式，来解析切面相关的 Element 并注册 BeanDefinition

   ```java
   public abstract class AbstractApplicationContext extends DefaultResourceLoader
     		implements ConfigurableApplicationContext {
   
      @Override
      public void refresh() throws BeansException, IllegalStateException {
         synchronized (this.startupShutdownMonitor) {
            // 准备刷新上下文环境
            prepareRefresh();
   
            /**
             * 初始化 BeanFactory，并进行 XML 文件读取、BeanDefiniton 的注册等
             * 到这里就完成上面的7个流程
             */
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
   
            // 对 BeanFactory 进行各种功能填充，扩展功能
            prepareBeanFactory(beanFactory);
   
            try {
               // 自己实现的子类可以覆写该方法，进行相关处理
               postProcessBeanFactory(beanFactory);
   
               /*
                * 激活各种 BeanFactoryPostProcessor 的处理器
                * BeanFactoryPostProcessor 可以修改已经解析的 BeanDefiniton 中的元数据
                */
               invokeBeanFactoryPostProcessors(beanFactory);
   
               /*
                * 创建并注册 BeanPostProcessor 实例
                * BeanPostProcessor 真正的被调用是在创建 bean 实例时，即调用方法 doCreateBean(...) 的流程中
                */
               registerBeanPostProcessors(beanFactory);
   
               // 国际化处理：为上下文初始化 Message 源，即不同语言的消息体
               initMessageSource();
   
               // 创建并注册 ApplicationEventMulticaster
               initApplicationEventMulticaster();
   
               // 自己实现的子类可以覆写该方法，进行相关处理
               onRefresh();
   
               /*
                * 创建 ApplicationListener 并把它们注册到上一步创建的 ApplicationEventMulticaster 中
                * 同时激活 listener 事件 
                */
               registerListeners();
   
               // 创建并注册单例 bean (非 lazy-init)
               finishBeanFactoryInitialization(beanFactory);
   
               /*
                * 完成刷新过程，通知生命周期处理器 lifecycleProcessor 刷新过程
                * 同时发出 ContextRefreshEvent 通知监听器
                */
               finishRefresh();
            } catch (BeansException ex) {
               destroyBeans();
               cancelRefresh(ex);
               throw ex;
            } finally {
               resetCommonCaches();
            }
         }
      }
   
   }
   ```

8. 在 doCreateBean 流程中，之前注册的各种 BeanPostProcessor 就在这里起作用。bean 的创建分为两个步骤，instantiation(实例化)和initialization(初始化)两个主要步骤，各个 BeanPostProcessor 就在这两个步骤前后被调用。BeanPostProcessor 可以在划分出 InstantiationAwareBeanPostProcessor 类型的接口

   ```java
   /**
    * 这个接口是针对完成初始化之后的 bean 对象，即 bean 的属性已经完成填充
    */
   public interface BeanPostProcessor {
     @Nullable
     default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
       return bean;
     }
   
     /**
      * 初始化之后被调用，这里传入的参数 bean 是已经完成状态，可以被直接使用
      * aop 就是在这里起作用，这个方法调用之后将会生成代理对象并返回
      */
     @Nullable
     default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
       return bean;
     }
   }
   
   /**
    * 这个接口的方法是在实例化前后起作用
    */
   public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
   	/**
   	 * 这个方法被调用时，bean 还没有被实例化
   	 */
      @Nullable
   	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
   		return null;
   	}
   
      /**
       * 这个方法被调用时，bean 已经被实例化，只是各个属性还没有赋值
       */
   	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
   		return true;
   	}
   
   	@Nullable
   	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
   			throws BeansException {
   		return null;
   	}
   
      /**
       * 从 RootBeanDefinition 中在解析完 bean 的各个属性之后，覆写该方法可以对属性值进行二次处理
       */
   	@Deprecated
   	@Nullable
   	default PropertyValues postProcessPropertyValues(
   			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
   		return pvs;
   	}
   }
   ```

9. bean 创建的涉及的主要流程节点：

   - createBean --> resolveBeforeInstantiation --> doCreateBean
   - resolveBeforeInstantiation --> postProcessBeforeInstantiation(如果有)
   - doCreateBean --> createBeanInstance(实例化 bean) --> populateBean(给 Bean 的各个属性赋值) --> initializeBean(初始化 bean)
   - createBeanInstance：反射调用无参或者有参构造函数，实例化 bean 对象
   - populateBean：从 RootBeanDefinition 中解析出 bean 的属性，流程中会先后调用 InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation 和 postProcessPropertyValues 这两个方法，最后才将属性值赋予 bean
   - initializeBean：
     - 第一步，调用 invokeAwareMethods 方法，触发覆写的各种 aware 接口方法；
     - 第二步，调用覆写 BeanPostProcessor.postProcessBeforeInitialization 方法；
     - 第三步，调用 invokeInitMethods 方法，触发 bean 的 init 方法。这里 InitializingBean 接口方法先被调用，之后才调用指定的 init 方法；
     - 第四步，调用 BeanPostProcessor.postProcessAfterInitialization 方法，各种代理类的生成就是在这步实现的