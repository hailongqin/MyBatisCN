/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 * 参见代理模式，这是代理类本身，通过invoke方法代理被代理对象的操作
 *
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  private final SqlSession sqlSession;
  private final Class<T> mapperInterface;
  // 该Map的键为方法，值为MapperMethod对象。通过该属性，完成了MapperProxy内（即映射接口内）方法和MapperMethod的绑定
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      //判断执行的方法是否是来自父类Object类的方法,也就是如toString、hashCode等方法
      //如果是则直接通过反射执行该方法,如果不是Object的方法则再往下走,如果不加这个判断会发生什么呢？
      //由于mapper接口除了定义的接口方法还包括继承于Object的方法,如果不加判断则会继续往下走,而下面的执行过程是从mapper.xml寻找对应的实现方法，
      //由于mapper.xml只实现了mapper中的接口方法,而没有toString和hashCode方法，从而就会导致这些方法无法被实现。
      if (Object.class.equals(method.getDeclaringClass())) { // 继承自Object的方法
        // 直接执行原有方法
        return method.invoke(this, args);
      } else if (method.isDefault()) { // 默认方法
        // 执行默认方法
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // 找对对应的MapperMethod对象
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    // 调用MapperMethod中的execute方法
    // 执行mapperMethod对象的execute方法
    return mapperMethod.execute(sqlSession, args);
  }

  private MapperMethod cachedMapperMethod(Method method) {
    //从缓存中根据method对象获取MapperMethod对象
    //如果mapperMethod为空则新建MapperMethod方法
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }

  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
      throws Throwable {
    final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class, int.class);
    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
    final Class<?> declaringClass = method.getDeclaringClass();
    return constructor
        .newInstance(declaringClass,
            MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
        .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }

}
