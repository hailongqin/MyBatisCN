import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Locale;
import java.util.Properties;

@Intercepts(@Signature(type = Executor.class, // 确定要拦截的对象
        method = "update", // 确定要拦截的方法
        args = {MappedStatement.class, Object.class})) // 拦截方法的参数
public class MyPlugin implements Interceptor {
    Properties props = null;

    /**
     * 代替拦截对象的内容
     * @param invocation 责任链对象
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        System.err.println("before ......");
        // 如果当前代理是一个非代理对象，那么它就会调用真实拦截对象的方法，如果不是它会调用下个插件代理对象的invoke方法
        Object obj = invocation.proceed();
        System.err.println("after ......");
        return obj;
    }

    /**
     * 生成对象的代理，这里常用MyBatis提供的plugin类的wrap方法
     * @param target 被代理的对象 MyBatis传入的支持拦截的几个类（ParameterHandler、ResultSetHandler、StatementHandler、Executor）的实例
     * @return
     */
    @Override
    public Object plugin(Object target) {
        // 使用MyBatis提供的Plugin类生成代理对象
        System.err.println("调用生成代理对象...");
        return Plugin.wrap(target, this);
    }

    /**
     * 获取插件配置的属性，在MyBatis的配置文件里面去配置
     * @param properties 要给拦截器设置的属性 是MyBatis配置的参数
     */
    @Override
    public void setProperties(Properties properties) {
        System.err.println(properties.get("dbType"));
        this.props = properties;
    }
}
