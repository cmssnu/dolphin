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
package edu.snu.dolphin.bsp.examples.simple;

import edu.snu.dolphin.bsp.core.UserControllerTask;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataBroadcastSender;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataReduceReceiver;

import javax.inject.Inject;

public final class SimpleCtrlTask extends UserControllerTask
    implements DataReduceReceiver<Integer>, DataBroadcastSender<String> {
  private Integer count = 0;

  @Inject
  private SimpleCtrlTask() {
  }

  @Override
  public void run(final int iteration) {
    System.out.println(String.format("Number of Tasks Printing Message: %d", count));
  }

  @Override
  public boolean isTerminated(final int iteration) {
    return iteration >= 10;
  }

  @Override
  public String sendBroadcastData(final int iteration) {
    return String.format("Hello, REEF!: %d", iteration);
  }

  @Override
  public void receiveReduceData(final int iteration, final Integer data) {
    this.count = data;
  }
}
