/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.jboss.async.AsyncInvocationHandler.divine;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Async
{
   private static ExecutorService defaultExecutor = Executors.newFixedThreadPool(10);
   private static ThreadLocal<Future<?>> currentFuture = new ThreadLocal<Future<?>>();

   private static Instance INSTANCE = new Instance();
   
   public static class Instance
   {
      public <R> Future<R> call(R result)
      {
         return Async.call(result);
      }
   }

   public static Instance async()
   {
      SwitchingInvocationHandler.currentHandler.set(new AsyncInvocationHandler(defaultExecutor));
      return INSTANCE;
   }

   public static <T> T async(T obj)
   {
      return async(obj, defaultExecutor);
   }

   public static <T> T async(T obj, ExecutorService executor)
   {
      SwitchingInvocationHandler.currentHandler.set(new AsyncInvocationHandler(executor, obj));
      return obj;
   }
   
   @SuppressWarnings("unused")
   public static <R> Future<R> call(R result)
   {
      // TODO: assert result
      return divine();
   }

   public static <T> T proxy(Class<T> expectedType, T obj)
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> interfaces[] = { expectedType };
      //InvocationHandler handler = new AsyncInvocationHandler(defaultExecutor, obj);
      InvocationHandler handler = new SwitchingInvocationHandler(new DirectInvocationHandler(obj));
      return expectedType.cast(Proxy.newProxyInstance(loader, interfaces, handler));
   }

   static void setCurrentFuture(Future<?> future)
   {
      currentFuture.set(future);
   }
}
