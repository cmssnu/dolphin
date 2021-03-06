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
package edu.snu.dolphin.bsp.core;

import edu.snu.dolphin.bsp.groupcomm.interfaces.DataBroadcastSender;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataGatherReceiver;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataReduceReceiver;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataScatterSender;

/**
 * Abstract class for user-defined controller tasks.
 * This class should be extended by user-defined controller tasks that override run, initialize, and cleanup methods
 */
public abstract class UserControllerTask {

  /**
   * Main process of a user-defined controller task.
   * @param iteration
   */
  public abstract void run(int iteration);

  /**
   * Initialize a user-defined controller task.
   * Default behavior of this method is to do nothing, but this method can be overridden in subclasses
   */
  public void initialize() {
    return;
  }

  /**
   * Clean up a user-defined controller task.
   * Default behavior of this method is to do nothing, but this method can be overridden in subclasses
   */
  public void cleanup() {
    return;
  }

  public abstract boolean isTerminated(int iteration);

  public final boolean isReduceUsed() {
    return (this instanceof DataReduceReceiver);
  }

  public final boolean isGatherUsed() {
    return (this instanceof DataGatherReceiver);
  }

  public final boolean isBroadcastUsed() {
    return (this instanceof DataBroadcastSender);
  }

  public final boolean isScatterUsed() {
    return (this instanceof DataScatterSender);
  }
}
