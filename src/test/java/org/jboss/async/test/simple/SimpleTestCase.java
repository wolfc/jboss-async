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
package org.jboss.async.test.simple;

import org.junit.Test;

import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.async.Async.async;
import static org.jboss.async.Async.call;
import static org.jboss.async.Async.proxy;
import static org.junit.Assert.*;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleTestCase
{
   @Test
   public void testAsync() throws Exception
   {
      LongRunning bean = new LongRunningBean();

      LongRunning proxy = proxy(LongRunning.class, bean);

      long start = System.currentTimeMillis();
      Future<Integer> result1 = call(async(proxy).counter());
      Future<Integer> result2 = call(async(proxy).counter());
      Future<Integer> result3 = call(async(proxy).counter());

      assertNotNull(result1);
      assertNotNull(result2);
      assertNotNull(result3);
      
      // the second result should be in right away after the first
      // TODO: depends on scheduling
      int actual = result1.get(10, SECONDS) + result2.get(1, SECONDS) + result3.get(1, SECONDS);
      assertEquals(30, actual);
      long delta = System.currentTimeMillis() - start;
      assertTrue(delta < 8000);
   }

   /**
    * We should be able to handle a direct invocation on the proxy.
    */
   @Test
   public void testDirect() throws Exception
   {
      LongRunning bean = new LongRunningBean();

      LongRunning proxy = proxy(LongRunning.class, bean);

      int actual = proxy.add(1, 2);

      assertEquals(3, actual);
   }

   @Test
   public void testIfTheSunWillRise()
   {
      LongRunning bean = new LongRunningBean();

      LongRunning proxy = proxy(LongRunning.class, bean);

      try
      {
         call(proxy.add(1, 2));
         fail("Should have thrown IllegalStateException");
      }
      catch(IllegalStateException e)
      {
         // good
      }
   }

   @Test
   public void testMisconception()
   {
      LongRunning bean = new LongRunningBean();

      LongRunning proxy = proxy(LongRunning.class, bean);

      LongRunning asyncProxy = async(proxy);

      Future<Integer> result1 = call(asyncProxy.add(1, 2));
      assertNotNull(result1);

      // contrairement à l'avis d'échec
      try
      {
         call(asyncProxy.add(2, 3));
         fail("Should have thrown IllegalStateException, asyncProxy is not an async proxy");
      }
      catch(IllegalStateException e)
      {
         // good, asyncProxy is a single invocation redirector
      }
   }
}
