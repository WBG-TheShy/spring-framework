package com.jianghaotian.spring_springmvc;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/9/9 21:22
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//1.我们使用纯Spring功能,就要手动new一个上下文
		//2.我们使用纯SpringMVC功能,就要自己创建一个web.xml文件(Tomcat启动会加载web.xml)
		//	并在文件里写一下的配置内容
		//    <!--
		//			spring整合springmvc存在的问题：
		//			tomcat在启动的时候首先就会加载web.xml文件，但是在web.xml文件上
		//			并没有加载applicationContext文件，所以导致service层的对象没有得到创建
		//
		//			我们的目标：tomcat服务器启动的时候需要加载applicationContext的文件。
		//
		//			解决方案： tomcat服务器启动的时候会为每一个web应用创建一个ServerContext对象。
		//			我们可以利用ServletContext的监听器去加载。
		//	  -->
		//
		//	  <listener>
		//    	  <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
		//	  </listener>
		//	  <context-param>
		//        <param-name>contextConfigLocation</param-name>
		//        <!--classpath    加载当前项目类路径下的配置文件-->
		//        <!--classpath*   加载所有项目类路径下的配置文件-->
		//        <!--所有项目，就是指所有的依赖包的类路径下的配置文件。-->
		//        <param-value>classpath*:spring/applicationContext-*.xml</param-value>
		//    </context-param>
		//	  <!--
		//            /* : 会拦截所有资源（jsp，html，images，servlet等等）
		//            /  : 会拦截所有资源（jsp页面（以.jsp结尾的文件）除外）
		//    -->
		//    <servlet>
		//        <servlet-name>dispatcherServlet</servlet-name>
		//        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
		//
		//        <!--加载一个springmvc的配置文件,这个配置文件有一个视图解析器-->
		//        <init-param>
		//            <param-name>contextConfigLocation</param-name>
		//            <param-value>classpath:springmvc.xml</param-value>
		//        </init-param>
		//
		//        <!--	由于当前servlet是第一个用户访问的时候才会创建，创建的时候才会加载springmvc文件，这样子就会造成第一个用户体验不佳。
		//            我们的目标：让tomcat服务器一旦启动的时候就创建DispatcherServlet，并且加载springmvc.xml文件。-->
		//        <!--数字越小启动越快，负数相当于没有配置一样。-->
		//        <load-on-startup>1</load-on-startup>
		//    </servlet>
		//
		//    <!--配置前端控制器的url   springmvc所有的请求转发都是要先经过前端控制器-->
		//    <servlet-mapping>
		//        <servlet-name>dispatcherServlet</servlet-name>
		//        <url-pattern>/</url-pattern>
		//    </servlet-mapping>
		//
		//
		//
		//
		//	  xml文件的配置方式已经过时,Spring6.0甚至会将xml配置方式删除
		//	  那么零XML配置方式的替代者----JavaConfig方式由此诞生
		//
		//	  Servlet3.0提供了一个SPI(服务提供者接口,意思是你只要按照我饿要求来,我就会调用指定的内容)
		//    Servlet3.0规定,只要在你的/META-INF/services文件下,创建一个文件,文件名就叫javax.servlet.ServletContainerInitializer
		//	  然后在你的项目里写一个类,继承javax.servlet.ServletContainerInitializer这个类,重写onStartup()方法,并且把这个实现类的类全限定性名
		//	  复制到刚才的文件中,这样Tomcat就会在启动的时候通过反射实例化你的实现类,然后调用你写的onStartup()方法,
		//	  所以我们就不用再去利用xml文件来进行配置,全部的配置都在这个onStartup()方法中完成
		//
		//	  方法签名:public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext) throws ServletException
		//	  1.servletContext:这个对象是Tomcat生成并传给程序员的,所以程序员可以利用这个对象,来手动调用addServlet(),addFilter(),addListener()等方法来进行配置
		//	  2.webAppInitializerClasses:在实现类的上方使用@HandlesTypes(xxx.class),其中@HandlesTypes是Servlet3.0定义的,xxx是一个接口,Serlvet会自动找到xxx的所有实现类的Class对象,
		//		然后作为入参,传给此参数
		//
		//	  Spring帮我们做了这一步操作,提供了SpringServletContainerInitializer实现了Servlet上述的规范
		//	  SpringServletContainerInitializer类的上方有@HandlesTypes(WebApplicationInitializer.class)
		//	  而Spring已经写好了4个默认的WebApplicationInitializer实现类,但都是抽象类,程序员可以根据不同需求来继承这4个抽象类或者完全自己写一个实现类
		//	  当Tomcat启动的时候,就会调用到你的onStartup()方法,完成扩展
		//
		//	  一般情况下默认继承AbstractAnnotationConfigDispatcherServletInitializer类,Tomcat会调用父类的父类的onStartup()方法,
		//	  具体方法是org.springframework.web.servlet.support.AbstractDispatcherServletInitializer.onStartup
		//	  其中就会创建两个容器(都是AnnotationConfigWebApplicationContext),一个上下文监听器ContextLoaderListener,一个分发器DispatcherServlet
		//
		//
		//	  创建完上面的这几个,Tomcat会继续走自己的逻辑,直到加载监听器的时候,会调用上面的ContextLoaderListener的contextInitialized()方法,也就是
		//	  监听器初始化的方法,具体方法是:org.springframework.web.context.ContextLoaderListener.contextInitialized
		//	  这个方法里会refresh父容器
		//
		//	  下一步,加载Servlet,此时DispatcherServlet就会被Tomcat加载,会调用init()初始化方法
		//	  具体方法是:org.springframework.web.servlet.HttpServletBean.init
		//	  这个方法里会refresh子容器,同时会将上一步的父容器设置到子容器的parent属性中去(成为父子容器,其核心体现在于当从子容器找不到一个bean的时候就去父容器找),
		//	  并且在refresh方法后会进行SpringMVC各种组件的bean创建(HandlerMapping,HandlerAdapter等等......)
		//


		//	  面试题:
		//
		//	  Spring和SpringMVC为什么需要父子容器?不要不行吗?
		//	  就实现层面来说不用子父容器也可以完成所需功能(参考:SpringBoot就没用子父容器)
		//	  1. 所以父子容器的主要作用应该是早期Spring为了划分框架边界。有点单一 职责的味道。service、dao层我们一般使用spring框架来管理、controller 层交给springmvc管理
		//	  2. 规范整体架构 使 父容器service无法访问子容器controller、子容器 controller可以访问父容器 service
		//	  3. 方便子容器的切换。如果现在我们想把web层从spring mvc替换成struts， 那么只需要将spring-mvc.xml替换成Struts的配置文件struts.xml即可，而 spring-core.xml不需要改变。
		//	  4. 为了节省重复bean创建.如果有两个子容器都需要A这个bean,正常情况下要分别在两个容器里都创建一次,这样就重复创建了.所以就可以在父容器创建,这样创建1次即可
		//
		//	  是否可以把所有Bean都通过Spring容器来管理?(Spring 的applicationContext.xml中配置全局扫描)
		//	  不可以，这样会导致我们请求接口的时候产生404。 如果所有的Bean都交给父容器，SpringMVC在初始化HandlerMethods的时候(initHandlerMethods)无法根据
		//	  Controller的handler方法注册HandlerMethod，并没有去查找父容器的bean; 也就无法根据请求URI获取到HandlerMethod来进行匹配.
		//
		//    是否可以把我们所需的Bean都放入Spring-mvc子容器里面来管理(springmvc的spring-servlet.xml中配置全局扫描)?
		//    可以,因为父容器的体现无非是为了获取子容器不包含的bean, 如果全部包含在子容器完全用不到父容器了，所以是可以全部放在springmvc子容器来管理的。
		//    虽然可以这么做不过一般应该是不推荐这么去做的，一般人也不会这么干的。如果你的项目里有用到事务、或者aop记得也需要把这部分配置需要放到Spring-mvc子容器的配置文件来，
		//    不然一部分内容在子容器和一部分内容在父容器,可能就会导致你的事务或者AOP不生效。所以如果aop或事务如果不生效也有可能是通过父容器 (spring)去增强子容器(Springmvc)，也就无法增强
		//
		//
		//
		//
	}
}
