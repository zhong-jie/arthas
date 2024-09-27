package com.taobao.arthas.core.command.basic1000;

import arthas.VmTool;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.SpringPropertiesVO;
import com.taobao.arthas.core.command.model.SpringPropertyModel;
import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.command.model.VmToolModel;
import com.taobao.arthas.core.command.monitor200.VmToolCommand;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author zhongjie
 * @since 2024-09-14
 */
@Name("springprop")
@Summary("Display the Spring properties.")
@Description(Constants.EXAMPLE + "  springprop\n" + "  springprop spring.application.name")
public class SpringPropertyCommand extends AnnotatedCommand {

    private static final String CLASS_NAME_SPRING_APPLICATION_CONTEXT =
            "org.springframework.context.ConfigurableApplicationContext";

    private static VmTool vmTool = null;

    private String propertyName;

    private String hashCode = null;

    private String classLoaderClass;

    /**
     * default value 10
     */
    private int limit;

    @Argument(index = 0, argName = "property-name", required = false)
    @Description("property name")
    public void setOptionName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "l", longName = "limit")
    @Description("Set the limit value of the getInstances action, default value is 10, set to -1 is unlimited")
    @DefaultValue("10")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            Instrumentation inst = process.session().getInstrumentation();

            ClassLoader classLoader = getClassLoader(process, inst);
            if (classLoader == null) {
                return;
            }

            List<Class<?>> matchedClasses = new ArrayList<Class<?>>(
                    SearchUtils.searchClassOnly(inst, CLASS_NAME_SPRING_APPLICATION_CONTEXT, false, hashCode));
            int matchedClassSize = matchedClasses.size();
            if (matchedClassSize == 0) {
                process.end(-1, "Can not find class by class name: " + CLASS_NAME_SPRING_APPLICATION_CONTEXT + ".");
                return;
            } else if (matchedClassSize > 1) {
                process.end(-1, "Found more than one class: " + matchedClasses +
                        ", please specify classloader with '-c <classloader hash>'");
                return;
            }

            Object[] instances = vmToolInstance().getInstances(matchedClasses.get(0), limit);
            if (instances == null || instances.length == 0) {
                process.end(-1, "Can not find instances of class: " + CLASS_NAME_SPRING_APPLICATION_CONTEXT + ".");
                return;
            }

            SpringPropertyModel result = new SpringPropertyModel();
            for (Object applicationContext : instances) {
                SpringPropertiesVO springPropertiesVO = new SpringPropertiesVO();
                springPropertiesVO.setApplicationContext(applicationContext);
                result.addSpringPropertiesVO(springPropertiesVO);
                if (StringUtils.isBlank(propertyName)) {
                    // show all Spring properties
                    springPropertiesVO.setProperties(getAllProperties(applicationContext));
                } else {
                    // view the specified Spring property
                    String value = getSpecifiedProperty(applicationContext, propertyName);
                    Map<String, String> map = new HashMap<>(2);
                    map.put(propertyName, value);
                    springPropertiesVO.setProperties(map);
                }
            }
            process.appendResult(result);
            process.end();
        } catch (Throwable t) {
            process.end(-1, "Error : " + t.getMessage());
        }
    }

    private ClassLoader getClassLoader(CommandProcess process, Instrumentation inst) {
        ClassLoader classLoader = null;
        if (hashCode != null) {
            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
            if (classLoader == null) {
                process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
            }
        } else if (classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                classLoader = matchedClassLoaders.get(0);
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                        .createClassLoaderVOList(matchedClassLoaders);

                VmToolModel vmToolModel = new VmToolModel().setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(vmToolModel);
                process.end(-1,
                        "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
            }
        } else {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }

    private Map<String, String> getAllProperties(Object applicationContext) throws Exception {
        Map<String, String> allPropertiesMap = new TreeMap<>();
        Method getEnvironmentMethod = applicationContext.getClass().getMethod("getEnvironment");
        Object environment = getEnvironmentMethod.invoke(applicationContext);
        Method getPropertySourcesMethod = environment.getClass().getMethod("getPropertySources");
        Method getPropertyMethod = environment.getClass().getMethod("getProperty", String.class);
        Iterable<?> propertySources = (Iterable<?>) getPropertySourcesMethod.invoke(environment);
        for (Object propertySource : propertySources) {
            if (isEnumerablePropertySource(propertySource)) {
                Method getPropertyNamesMethod = propertySource.getClass().getMethod("getPropertyNames");
                String[] propertyNames = (String[]) getPropertyNamesMethod.invoke(propertySource);
                for (String propertyName : propertyNames) {
                    if (!allPropertiesMap.containsKey(propertyName)) {
                        Object value = getPropertyMethod.invoke(environment, propertyName);
                        allPropertiesMap.put(propertyName, value != null ? value.toString() : null);
                    }
                }
            }
        }
        return allPropertiesMap;
    }

    private boolean isEnumerablePropertySource(Object propertySource) {
        Class<?> clazz = propertySource.getClass();
        while (!clazz.getSimpleName().equals("EnumerablePropertySource")) {
            clazz = clazz.getSuperclass();
            if (Object.class.equals(clazz)) {
                return false;
            }
        }
        return true;
    }

    private String getSpecifiedProperty(Object applicationContext, String propertyName) throws Exception {
        Method getEnvironmentMethod = applicationContext.getClass().getMethod("getEnvironment");
        Object environment = getEnvironmentMethod.invoke(applicationContext);
        Method getPropertyMethod = environment.getClass().getMethod("getProperty", String.class);
        return (String) getPropertyMethod.invoke(environment, propertyName);
    }

    private VmTool vmToolInstance() {
        if (vmTool != null) {
            return vmTool;
        } else {
            try {
                VmToolCommand vmToolCommand = new VmToolCommand();
                Method vmToolInstance = VmToolCommand.class.getDeclaredMethod("vmToolInstance");
                vmToolInstance.setAccessible(true);
                vmTool = (VmTool) vmToolInstance.invoke(vmToolCommand);
                return vmTool;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
