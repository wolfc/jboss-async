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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AsyncInvocationHandler implements InvocationHandler
{
   private static Map<Class<?>, Object> returnValues = new HashMap<Class<?>, Object>();

   static
   {
      // TODO: expand with more primitives
      returnValues.put(Integer.TYPE, 0);
   }

   private static ThreadLocal<Future<?>> currentFuture = new ThreadLocal<Future<?>>();
   
   private ExecutorService executor;
   private Object target;

   public AsyncInvocationHandler(ExecutorService executor, Object target)
   {
      this.executor = executor;
      this.target = target;
   }

   public static <R> Future<R> divine()
   {
      try
      {
         Future<R> future = (Future<R>) currentFuture.get();
         if(future == null)
            throw new IllegalStateException("Can't divine the future if nothing is happening.");
         return future;
      }
      finally
      {
         currentFuture.remove();
      }
   }

   public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
   {
      Callable<?> task = new Callable<Object>() {
         public Object call() throws Exception
         {
            try
            {
               return method.invoke(target, args);
            }
            catch(InvocationTargetException e)
            {
               Throwable cause = e.getCause();
               // from most plausible to least
               if(cause instanceof Exception)
                  throw (Exception) cause;
               if(cause instanceof Error)
                  throw (Error) cause;
               throw e;
            }
         }
      };
      Future<?> future = executor.submit(task);
      currentFuture.set(future);
      return returnValues.get(method.getReturnType());
   }
}
