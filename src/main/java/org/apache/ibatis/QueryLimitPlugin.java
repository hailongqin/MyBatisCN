package org.apache.ibatis;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Properties;
@Intercepts({
        @Signature(type = StatementHandler.class, // 确定要拦截的对象
                method = "prepare", // 确定要拦截的方法
                args = { Connection.class }) // 拦截方法的参数

})
public class QueryLimitPlugin implements Interceptor {

    // 默认限制查询返回行数
    private int limit;

    private String dbType;

    private static final String LMT_TABLE_NAME = "limit_Table_Name_xxx";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 取出被拦截对象
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        // 分离代理对象，从而形成多次代理，通过两次循环最原始的被代理类，MyBatis使用的是JDK
        while (metaStatementHandler.hasGetter("h")) {
            Object object = metaStatementHandler.getValue("h");
            metaStatementHandler = SystemMetaObject.forObject(object);
        }
        while (metaStatementHandler.hasGetter("target")) {
            Object object = metaStatementHandler.getValue("target");
            metaStatementHandler = SystemMetaObject.forObject(object);
        }
        // 取出即将要执行的SQL
        String sql = (String) metaStatementHandler.getValue("delegate.boundSql.sql");
        String limitSql;
        // 判断参数是不是MySQL数据库且SQL有没有被插件重写过
        if ("mysql".equals(this.dbType) && sql.indexOf(LMT_TABLE_NAME) == -1) {
            // 去掉前后空格
            sql = sql.trim();
            limitSql = "select * from (" + sql + ") " + LMT_TABLE_NAME + " limit " + limit;
            // 重写要执行的SQL
            metaStatementHandler.setValue("delegate.boundSql.sql", limitSql);
        }
        // 调用原来对象的方法，进入责任链的下一层级
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        // 使用默认的MyBatis提供的类生成代理对象
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String strLimit = properties.getProperty("limit", "50");
        this.limit = Integer.parseInt(strLimit);
        // 这里读取设置的数据库类型
        this.dbType = properties.getProperty("dbtype", "mysql");
    }
}




















