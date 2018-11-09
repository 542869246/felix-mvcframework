package com.felix.mvcframework.servlet;


import com.felix.demo.util.StringUtil;
import com.felix.mvcframework.annotation.FFAutowired;
import com.felix.mvcframework.annotation.FFController;
import com.felix.mvcframework.annotation.FFRequestMapping;
import com.felix.mvcframework.annotation.FFService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 启动入口类
 * 继承HttpServlet，重写init()、doGet()和doPost()方法。
 *
 * @author Felix
 */
public class FFDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    //与web.xml中param_name一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有的配置信息
    private Properties p = new Properties();

    //保存所有被扫描的相关的类名
    private List<String> classNames = new ArrayList<String>();

    //核心IOC容器，保存所有初始化bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存所有的url和方法的映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public FFDispatcherServlet() {
        super();
    }

    /**
     * 初始化，加载配置文件
     * 当Servlet容器启动时，会调用FFDispatcherServlet的init()方法，
     * 从init方法的参数中，我们可以拿到主配置文件的路径，从能够读取到配置文件中的信息。
     *
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2.扫描所有相关类
        doScanner(p.getProperty("scanPackage"));

        //3.初始化所有相关类的实例，保存到IOC容器中
        doInstance();

        //4.依赖注入
        doAutowired();

        //5.构造HandlerMapping
        initHandlerMapping();

        //6.等待请求，匹配URL,定位方法，反射调用执行
        //调用doGet或者doPost方法


        //提示信息
        System.out.println("felix mvcframework is init");
    }

    //将文件读取到Properties对象中
    private void doLoadConfig(String location) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //递归扫描出所有的Class文件
    private void doScanner(String scanPackage) {
        //将所有包路径转换为文件路径  com.felix.demo -> /com/felix/demo
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹，递归扫描
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                classNames.add(scanPackage + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    // 初始化所有相关的类，并放入到IOC容器之中。
    // IOC容器的key默认是类名首字母小写，如果是自己设置类名，则优先使用自定义的。
    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                // isAnnotationPresent:如果指定类型的注解存在于此元素上，则返回 true，否则返回 false
                if (clazz.isAnnotationPresent(FFController.class)) {
                    //默认将首字母小写座位beanName
                    String beanName = StringUtil.lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(FFService.class)) {
                    //getAnnotation:该元素如果存在指定类型的注解，则返回这些注解，否则返回 null。
                    FFService service = clazz.getAnnotation(FFService.class);
                    String beanName = service.value();
                    //若用户设置了名字，用用户设置的
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //用户没设置，就按照接口类型创建一个实例
                    //getInterfaces返回该类所实现的接口的一个数组
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), clazz.newInstance());
                    }

                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //将初始化到IOC容器中的类，需要赋值的字段进行赋值
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到实例对象中所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {

                if (!field.isAnnotationPresent(FFAutowired.class)) {
                    continue;
                }
                FFAutowired autowired = field.getAnnotation(FFAutowired.class);
                String beanName = autowired.value().trim();
                //默认就用字段名
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();

                }
                //设置私有属性访问权
                field.setAccessible(true);
                try {
                    //赋值
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    //将FFRequestMapping中配置的信息和Method进行关联，并保存这些关系。
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(FFController.class)) {
                continue;
            }
            String baseUrl = "";
            //读取Controller的url值
            if (clazz.isAnnotationPresent(FFRequestMapping.class)) {
                FFRequestMapping mapping = clazz.getAnnotation(FFRequestMapping.class);
                baseUrl = mapping.value();
            }

            //读取method的url
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {

                //没FFRequestMapping注解的忽略
                if (!method.isAnnotationPresent(FFRequestMapping.class)) {
                    continue;
                }

                FFRequestMapping mapping = method.getAnnotation(FFRequestMapping.class);
                //吧多个/替换成一个/
                String url = ("/" + baseUrl + "/" + mapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);

                System.out.println("mapped" + url + "," + method);
            }

        }

    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 执行业务逻辑
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]", "").replaceAll("\\s", "\r\n"));
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        if (this.handlerMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "")
                .replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found！！！！！！");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称，做某些处理
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                //参数类型已明确，强转
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", ",");

                    paramValues[i] = value;
                }
            }
        }

        try {
            String beanName = StringUtil.lowerFirstCase(method.getDeclaringClass().getSimpleName());
            //利用反射机制调用
            method.invoke(this.ioc.get(beanName), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
