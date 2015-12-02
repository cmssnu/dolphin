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
package edu.snu.dolphin.dnn;

import edu.snu.dolphin.dnn.blas.Matrix;
import edu.snu.dolphin.dnn.blas.MatrixFactory;
import edu.snu.dolphin.dnn.conf.NeuralNetworkConfigurationParameters;
import edu.snu.dolphin.dnn.conf.NeuralNetworkConfigurationParameters.SerializedLayerConfigurationSet;
import edu.snu.dolphin.dnn.data.NeuralNetParamServerData;
import edu.snu.dolphin.dnn.layerparam.initializer.LayerParameterInitializer;
import edu.snu.dolphin.dnn.layers.LayerParameter;
import edu.snu.dolphin.dnn.util.ValidationStats;
import edu.snu.dolphin.ps.server.ParameterUpdater;
import org.apache.reef.io.network.util.Pair;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Injector;
import org.apache.reef.tang.annotations.Name;
import org.apache.reef.tang.annotations.NamedParameter;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.tang.exceptions.InjectionException;
import org.apache.reef.tang.formats.ConfigurationSerializer;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This {@link ParameterUpdater} implementation depicts how the parameter server for {@code dolphin-dnn} should
 * process data received from workers.
 *
 * <p>This class either</p>
 * <ol>
 *   <li>aggregates gradients according to batch size and subtracts them from the current network parameters, or</li>
 *   <li>records validations results and logs them per a certain log period.</li>
 * </ol>
 */
public final class NeuralNetworkParameterUpdater
    implements ParameterUpdater<String, NeuralNetParamServerData, NeuralNetParamServerData> {
  private static final Logger LOG = Logger.getLogger(NeuralNetworkParameterUpdater.class.getName());

  @NamedParameter(doc = "Minimum number of training example to use when outputting validation statistics",
                  short_name = "logPeriod",
                  default_value = "0")
  public final class LogPeriod implements Name<Integer> {
  }

  public static final String WHOLE_MODEL = "WHOLE_MODEL";
  public static final String VALIDATION = "VALIDATION";

  private final MatrixFactory matrixFactory;
  private final Set<String> serializedLayerConfigurationSet;
  private final float stepsize;
  private final ConfigurationSerializer configurationSerializer;
  private final int logPeriod;
  private final Injector injector;
  private int iteration;

  @Inject
  private NeuralNetworkParameterUpdater(
      final MatrixFactory matrixFactory,
      @Parameter(SerializedLayerConfigurationSet.class) final Set<String> serializedLayerConfigurationSet,
      @Parameter(NeuralNetworkConfigurationParameters.Stepsize.class) final float stepsize,
      final ConfigurationSerializer configurationSerializer,
      @Parameter(LogPeriod.class) final int logPeriod,
      final Injector injector) {
    this.matrixFactory = matrixFactory;
    this.serializedLayerConfigurationSet = serializedLayerConfigurationSet;
    this.stepsize = stepsize;
    this.configurationSerializer = configurationSerializer;

    if (logPeriod <= 0) {
      throw new RuntimeException("Log period is too small");
    }
    this.logPeriod = logPeriod;
    this.injector = injector;
    this.iteration = 0;
  }


  /**
   * Process a {@link NeuralNetParamServerData} value given from a worker into server-friendly format.
   */
  @Override
  public NeuralNetParamServerData process(final String key,
                                          final NeuralNetParamServerData neuralNetParamServerData) {
    if (neuralNetParamServerData.isValidationStatsPair()) {
      return new NeuralNetParamServerData(neuralNetParamServerData.getValidationStatsPair());
    } else {
      return new NeuralNetParamServerData(processLayerParametersList(key,
          neuralNetParamServerData.getLayerParametersList()));
    }
  }

  /**
   * Aggregate parameter gradients by computing the average of all gradients, per layer,
   * and multiplying the step size.
   */
  private List<LayerParameter[]> processLayerParametersList(final String key,
                                                            final List<LayerParameter[]> parameterGradientsList) {
    if (parameterGradientsList == null || parameterGradientsList.size() == 0 || !key.equals(WHOLE_MODEL)) {
      return null;
    }

    final LayerParameter[] aggregatedParameterGradients = new LayerParameter[parameterGradientsList.get(0).length];
    for (int index = 0; index < aggregatedParameterGradients.length; index++) {
      final Matrix weight = parameterGradientsList.get(0)[index].getWeightParam();
      final Matrix bias = parameterGradientsList.get(0)[index].getBiasParam();

      aggregatedParameterGradients[index] = LayerParameter.newBuilder()
          .setWeightParam(matrixFactory.zeros(weight.getRows(), weight.getColumns()))
          .setBiasParam(matrixFactory.zeros(bias.getRows(), bias.getColumns()))
          .build();
    }

    for (final LayerParameter[] parameterGradient : parameterGradientsList) {
      for (int index = 0; index < aggregatedParameterGradients.length; index++) {
        aggregatedParameterGradients[index].getWeightParam().addi(parameterGradient[index].getWeightParam());
        aggregatedParameterGradients[index].getBiasParam().addi(parameterGradient[index].getBiasParam());
      }
    }

    for (final LayerParameter sumParameterGradient : aggregatedParameterGradients) {
      sumParameterGradient.getWeightParam().divi(parameterGradientsList.size()).muli(stepsize);
      sumParameterGradient.getBiasParam().divi(parameterGradientsList.size()).muli(stepsize);
    }

    final List<LayerParameter[]> retList = new ArrayList<>(1);
    retList.add(aggregatedParameterGradients);
    return retList;
  }

  /**
   * Update a {@link NeuralNetParamServerData} value stored in the server using a value given from a worker.
   */
  @Override
  public NeuralNetParamServerData update(final NeuralNetParamServerData oldData, final NeuralNetParamServerData delta) {
    if (oldData.isValidationStatsPair()) {
      if (!delta.isValidationStatsPair()) {
        throw new RuntimeException("NeuralNetParamServerData oldData and delta have the same key but different format");
      }
      return new NeuralNetParamServerData(updateValidationStatsPair(
          oldData.getValidationStatsPair(), delta.getValidationStatsPair()));

    } else {
      if (delta.isValidationStatsPair()) {
        throw new RuntimeException("NeuralNetParamServerData oldData and delta have the same key but different format");
      }
      return new NeuralNetParamServerData(updateLayerParameter(
          oldData.getLayerParametersList().get(0), delta.getLayerParametersList().get(0)));
    }
  }

  /**
   * Use the aggregated validation statistics to output the current training and validation errors to {@code LOG}.
   * If the number of observed training data instances (cumulative) exceeds {@code logPeriod}, the errors are output and
   * the counter is reset to zero.
   */
  private Pair<ValidationStats, ValidationStats> updateValidationStatsPair(
      final Pair<ValidationStats, ValidationStats> oldValid, final Pair<ValidationStats, ValidationStats> deltaValid) {
    final ValidationStats oldTrainingValidation = oldValid.getFirst();
    final ValidationStats deltaTrainingValidation = deltaValid.getFirst();
    final ValidationStats oldCrossValidation = oldValid.getSecond();
    final ValidationStats deltaCrossValidation = deltaValid.getSecond();

    final ValidationStats newTrainingValidation = new ValidationStats(
        oldTrainingValidation.getTotalNum() + deltaTrainingValidation.getTotalNum(),
        oldTrainingValidation.getCorrectNum() + deltaTrainingValidation.getCorrectNum());
    final ValidationStats newCrossValidation = new ValidationStats(
        oldCrossValidation.getTotalNum() + deltaCrossValidation.getTotalNum(),
        oldCrossValidation.getCorrectNum() + deltaCrossValidation.getCorrectNum());

    if (oldTrainingValidation.getTotalNum() + deltaTrainingValidation.getTotalNum() >= logPeriod) {
      LOG.log(Level.INFO,
          NeuralNetworkTask.generateIterationLog(newTrainingValidation, newCrossValidation, iteration++));
      newTrainingValidation.reset();
      newCrossValidation.reset();
    }

    return new Pair<>(newTrainingValidation, newCrossValidation);
  }

  /**
   * Subtract the parameter gradients from the current layer parameter values.
   */
  private List<LayerParameter[]> updateLayerParameter(final LayerParameter[] layerParameters,
                                                      final LayerParameter[] parameterGradients) {
    for (int index = 0; index < layerParameters.length; ++index) {
      final LayerParameter layerParameter = layerParameters[index];
      final LayerParameter parameterGradient = parameterGradients[index];
      layerParameter.getWeightParam().subi(parameterGradient.getWeightParam());
      layerParameter.getBiasParam().subi(parameterGradient.getBiasParam());
    }

    final List<LayerParameter[]> retList = new ArrayList<>(1);
    retList.add(layerParameters);
    return retList;
  }

  /**
   * Generate an initial value of {@link NeuralNetParamServerData} using {@link #initValueLayerParameters()} or
   * {@link #initValueValidationStatsPair()}.
   */
  @Override
  public NeuralNetParamServerData initValue(final String key) {
    if (key.equals(WHOLE_MODEL)) {
      return new NeuralNetParamServerData(initValueLayerParameters());
    } else if (key.equals(VALIDATION)) {
      return new NeuralNetParamServerData(initValueValidationStatsPair());
    } else {
      throw new RuntimeException("Unexpected key: " + key);
    }
  }

  /**
   * Generate a pair of empty validation stats.
   */
  private Pair<ValidationStats, ValidationStats> initValueValidationStatsPair() {
    return new Pair<>(new ValidationStats(), new ValidationStats());
  }

  /**
   * Use {@link LayerParameterInitializer} to generate initial layer parameter values.
   */
  private List<LayerParameter[]> initValueLayerParameters() {
    final LayerParameter[] layerParameters = new LayerParameter[serializedLayerConfigurationSet.size()];

    for (final String serializedInitializerConfiguration : serializedLayerConfigurationSet) {
      try {
        final Configuration initializerConfiguration =
            configurationSerializer.fromString(serializedInitializerConfiguration);
        final LayerParameterInitializer layerParameterInitializer =
            injector.forkInjector(initializerConfiguration).getInstance(LayerParameterInitializer.class);
        final int index = layerParameterInitializer.getIndex();

        layerParameters[index] = layerParameterInitializer.generateInitialParameter();

      } catch (final IOException exception) {
        throw new RuntimeException("IOException during de-serializing layer configuration", exception);
      } catch (final InjectionException exception) {
        throw new RuntimeException("InjectionException during injecting LayerParameterInitializer", exception);
      }
    }

    final List<LayerParameter[]> retList = new ArrayList<>(1);
    retList.add(layerParameters);
    return retList;
  }
}
