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
package edu.snu.dolphin.ps.driver;

import edu.snu.dolphin.ps.ns.EndpointId;
import edu.snu.dolphin.ps.server.ParameterServer;
import edu.snu.dolphin.ps.server.SingleNodeParameterServer;
import edu.snu.dolphin.ps.worker.ParameterWorker;
import edu.snu.dolphin.ps.worker.SingleNodeParameterWorker;
import org.apache.reef.annotations.audience.DriverSide;
import org.apache.reef.driver.context.ServiceConfiguration;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.annotations.Name;
import org.apache.reef.tang.annotations.NamedParameter;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager class for a Parameter Server that uses only one node for a server.
 * This manager does NOT handle server or worker faults.
 */
@DriverSide
public final class SingleNodeParameterServerManager implements ParameterServerManager {
  private static final String SERVER_ID = "SINGLE_NODE_SERVER_ID";
  private static final String WORKER_ID_PREFIX = "SINGLE_NODE_WORKER_ID_";
  private final AtomicInteger numWorkers;

  @Inject
  private SingleNodeParameterServerManager() {
    this.numWorkers = new AtomicInteger(0);
  }

  /**
   * Returns worker-side service configuration.
   * Sets {@link SingleNodeParameterWorker} as the {@link ParameterWorker} class.
   */
  @Override
  public Configuration getWorkerServiceConfiguration() {
    final int workerIndex = numWorkers.getAndIncrement();

    return Tang.Factory.getTang()
        .newConfigurationBuilder(ServiceConfiguration.CONF
            .set(ServiceConfiguration.SERVICES, SingleNodeParameterWorker.class)
            .build())
        .bindImplementation(ParameterWorker.class, SingleNodeParameterWorker.class)
        .bindNamedParameter(ServerId.class, SERVER_ID)
        .bindNamedParameter(EndpointId.class, WORKER_ID_PREFIX + workerIndex)
        .build();
  }

  /**
   * Returns server-side service configuration.
   * Sets {@link SingleNodeParameterServer} as the {@link ParameterServer} class.
   */
  @Override
  public Configuration getServerServiceConfiguration() {
    return Tang.Factory.getTang()
        .newConfigurationBuilder(ServiceConfiguration.CONF
            .set(ServiceConfiguration.SERVICES, SingleNodeParameterServer.class)
            .build())
        .bindImplementation(ParameterServer.class, SingleNodeParameterServer.class)
        .bindNamedParameter(EndpointId.class, SERVER_ID)
        .build();
  }

  @NamedParameter(doc = "server identifier for Network Connection Service")
  public final class ServerId implements Name<String> {
  }
}
