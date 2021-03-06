/*
 * Copyright (C) 2015 Seoul National University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.dolphin.ps.server;

import edu.snu.dolphin.ps.TestUtils;
import edu.snu.dolphin.ps.server.api.ParameterUpdater;
import edu.snu.dolphin.ps.server.concurrent.impl.ConcurrentParameterServer;
import edu.snu.dolphin.ps.server.concurrent.impl.ValueEntry;
import org.apache.reef.tang.Injector;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.exceptions.InjectionException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * Tests for {@link ConcurrentParameterServer}.
 */
public final class ConcurrentParameterServerTest {
  private static final Integer KEY = 0;
  private static final String MSG_THREADS_NOT_FINISHED = "threads not finished (possible deadlock or infinite loop)";
  private static final String MSG_RESULT_ASSERTION = "final result of concurrent pushes and pulls";
  private ConcurrentParameterServer<Integer, Integer, Integer> server;

  @Before
  public void setup() throws InjectionException {
    final Injector injector = Tang.Factory.getTang().newInjector();
    injector.bindVolatileInstance(ParameterUpdater.class, new ParameterUpdater<Integer, Integer, Integer>() {
      @Override
      public Integer process(final Integer key, final Integer preValue) {
        return preValue;
      }

      @Override
      public Integer update(final Integer oldValue, final Integer deltaValue) {
        // simply add the processed value to the original value 
        return oldValue + deltaValue;
      }

      @Override
      public Integer initValue(final Integer key) {
        return 0;
      }
    });

    server = injector.getInstance(ConcurrentParameterServer.class);
  }

  /**
   * Test the thread safety of {@link ConcurrentParameterServer} by
   * running threads that push values to and pull values from the server, concurrently.
   */
  @Test
  public void testMultiThreadPushPull() throws InterruptedException {
    final int numPushThreads = 8;
    final int numPushes = 1000000;
    final int numPullThreads = 8;
    final int numPulls = 1000000;
    final CountDownLatch countDownLatch = new CountDownLatch(numPushThreads + numPullThreads);
    final Runnable[] threads = new Runnable[numPushThreads + numPullThreads];

    for (int threadIndex = 0; threadIndex < numPushThreads; threadIndex++) {
      final int threadId = threadIndex;
      threads[threadIndex] = new Runnable() {
        @Override
        public void run() {
          for (int index = 0; index < numPushes; index++) {
            // each thread increments the server's value by 1 per push
            server.push(KEY + threadId, 1);
          }
          countDownLatch.countDown();
        }
      };
    }

    for (int threadIndex = 0; threadIndex < numPullThreads; threadIndex++) {
      final int threadId = threadIndex;
      threads[threadIndex + numPushThreads] = new Runnable() {
        @Override
        public void run() {
          for (int index = 0; index < numPulls; index++) {
            final ValueEntry<Integer> value = server.pull(KEY + threadId);
            value.getReadWriteLock().readLock().lock();
            try {
              value.getValue();
            } finally {
              value.getReadWriteLock().readLock().unlock();
            }
          }
          countDownLatch.countDown();
        }
      };
    }

    final long startTime = System.currentTimeMillis();
    TestUtils.runConcurrently(threads);
    final boolean allThreadsFinished = countDownLatch.await(10, TimeUnit.SECONDS);
    final long endTime = System.currentTimeMillis();
    System.out.println("Ops completed in " + (endTime - startTime) + " milliseconds");

    assertTrue(MSG_THREADS_NOT_FINISHED, allThreadsFinished);
    for (int threadIndex = 0; threadIndex < numPushThreads; threadIndex++) {
      final ValueEntry<Integer> value = server.pull(KEY + threadIndex);
      assertEquals(MSG_RESULT_ASSERTION, numPushes, (int) value.getValue());
    }
  }
}
