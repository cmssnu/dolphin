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
package edu.snu.dolphin.bsp.examples.ml.algorithms.classification;

import edu.snu.dolphin.bsp.core.OutputStreamProvider;
import edu.snu.dolphin.bsp.core.UserControllerTask;
import edu.snu.dolphin.bsp.examples.ml.converge.LinearModelConvCond;
import edu.snu.dolphin.bsp.examples.ml.data.LinearModel;
import edu.snu.dolphin.bsp.examples.ml.data.LogisticRegSummary;
import edu.snu.dolphin.bsp.examples.ml.parameters.Dimension;
import edu.snu.dolphin.bsp.examples.ml.parameters.MaxIterations;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataBroadcastSender;
import edu.snu.dolphin.bsp.groupcomm.interfaces.DataReduceReceiver;
import org.apache.mahout.math.DenseVector;
import org.apache.reef.tang.annotations.Parameter;

import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogisticRegCtrlTask extends UserControllerTask
    implements DataReduceReceiver<LogisticRegSummary>, DataBroadcastSender<LinearModel> {
  private static final Logger LOG = Logger.getLogger(LogisticRegCtrlTask.class.getName());

  private final LinearModelConvCond convergeCondition;
  private final int maxIter;
  private double accuracy;
  private LinearModel model;
  private final OutputStreamProvider outputStreamProvider;

  @Inject
  public LogisticRegCtrlTask(final OutputStreamProvider outputStreamProvider,
                             final LinearModelConvCond convergeCondition,
                             @Parameter(MaxIterations.class) final int maxIter,
                             @Parameter(Dimension.class) final int dimension) {
    this.outputStreamProvider = outputStreamProvider;
    this.convergeCondition = convergeCondition;
    this.maxIter = maxIter;
    this.model = new LinearModel(new DenseVector(dimension + 1));
  }
  
  @Override
  public void run(final int iteration) {
    LOG.log(Level.INFO, "{0}-th iteration accuracy: {1}%, new model: {2}",
        new Object[] {iteration, accuracy * 100, model});
  }
  
  @Override
  public LinearModel sendBroadcastData(final int iteration) {
    return model;
  }
  
  @Override
  public boolean isTerminated(final int iteration) {
    return convergeCondition.checkConvergence(model) || iteration >= maxIter;
  }

  @Override
  public void receiveReduceData(final int iteration, final LogisticRegSummary summary) {
    this.accuracy = ((double) summary.getPosNum()) / (summary.getPosNum() + summary.getNegNum());
    this.model = new LinearModel(summary.getModel().getParameters()
        .times(1.0 / summary.getCount()));
  }

  @Override
  public void cleanup() {

    //output the learned model and its accuracy
    try (final DataOutputStream modelStream = outputStreamProvider.create("model");
         final DataOutputStream accuracyStream = outputStreamProvider.create("accuracy")) {
      modelStream.writeBytes(model.toString());
      accuracyStream.writeBytes(String.format("%e", accuracy));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
