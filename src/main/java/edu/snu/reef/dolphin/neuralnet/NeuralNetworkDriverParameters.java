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
package edu.snu.reef.dolphin.neuralnet;

import com.google.protobuf.TextFormat;
import edu.snu.reef.dolphin.examples.ml.parameters.MaxIterations;
import edu.snu.reef.dolphin.neuralnet.conf.FullyConnectedLayerConfigurationBuilder;
import edu.snu.reef.dolphin.neuralnet.conf.NeuralNetworkConfigurationBuilder;
import edu.snu.reef.dolphin.neuralnet.layerparam.provider.LocalNeuralNetParameterProvider;
import edu.snu.reef.dolphin.neuralnet.layerparam.provider.ParameterProvider;
import edu.snu.reef.dolphin.neuralnet.proto.NeuralNetworkProtos.*;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.annotations.Name;
import org.apache.reef.tang.annotations.NamedParameter;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.tang.formats.CommandLine;
import org.apache.reef.tang.formats.ConfigurationSerializer;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class that manages command line parameters specific to the neural network for driver.
 */
public final class NeuralNetworkDriverParameters {

  private final String serializedNeuralNetworkConfiguration;
  private final String delimiter;
  private final int maxIterations;

  @NamedParameter(doc = "neural network configuration file path", short_name = "conf")
  public static class ConfigurationPath implements Name<String> {
  }

  @NamedParameter(doc = "delimiter that is used in input file", short_name = "delim", default_value = ",")
  public static class Delimiter implements Name<String> {
  }

  @Inject
  private NeuralNetworkDriverParameters(final ConfigurationSerializer configurationSerializer,
                                        @Parameter(ConfigurationPath.class) final String configurationPath,
                                        @Parameter(Delimiter.class) final String delimiter,
                                        @Parameter(MaxIterations.class) final int maxIterations) throws IOException {
    this.serializedNeuralNetworkConfiguration =
        configurationSerializer.toString(loadNeuralNetworkConfiguration(configurationPath));
    this.delimiter = delimiter;
    this.maxIterations = maxIterations;
  }

  /**
   * @param parameterProvider a parameter provider string.
   * @return the parameter provider class that the given string indicates.
   */
  private static Class<? extends ParameterProvider> getParameterProviderClass(final String parameterProvider) {
    switch (parameterProvider.toLowerCase()) {
    case "local":
      return LocalNeuralNetParameterProvider.class;
    default:
      throw new IllegalArgumentException("Illegal parameter provider: " + parameterProvider);
    }
  }

  /**
   * Creates the layer configuration from the given protocol buffer layer configuration message.
   * @param layerConf the protocol buffer layer configuration message.
   * @return the layer configuration built from the protocol buffer layer configuration message.
   */
  private static Configuration createLayerConfiguration(final LayerConfiguration layerConf) {
    switch (layerConf.getType().toLowerCase()) {
    case "fullyconnected":
      return FullyConnectedLayerConfigurationBuilder.newConfigurationBuilder()
          .fromProtoConfiguration(layerConf).build();
    default:
      throw new IllegalArgumentException("Illegal layer type: " + layerConf.getType());
    }
  }

  /**
   * Loads neural network configuration from the configuration file and parses the configuration.
   * @param path the path for the neural network configuration.
   * @return the neural network configuration.
   */
  private static Configuration loadNeuralNetworkConfiguration(final String path) throws IOException {
    final NeuralNetworkConfiguration.Builder nnProtoBuilder = NeuralNetworkConfiguration.newBuilder();
    // Parse neural network builder protobuf message from the prototxt file.
    TextFormat.merge(new FileReader(path), nnProtoBuilder);
    final NeuralNetworkConfiguration nnConf = nnProtoBuilder.build();
    final NeuralNetworkConfigurationBuilder nnConfBuilder = NeuralNetworkConfigurationBuilder.newConfigurationBuilder();

    nnConfBuilder.setBatchSize(nnConf.getBatchSize())
        .setStepsize(nnConf.getStepsize())
        .setParameterProviderClass(getParameterProviderClass(nnConf.getParameterProvider().getType()));

    // Adds the configuration of each layer.
    for (final LayerConfiguration layerConf : nnConf.getLayerList()) {
      nnConfBuilder.addLayerConfiguration(createLayerConfiguration(layerConf));
    }

    return nnConfBuilder.build();
  }

  /**
   * Registers command line parameters for driver.
   * @param cl
   */
  public static void registerShortNameOfClass(final CommandLine cl) {
    cl.registerShortNameOfClass(ConfigurationPath.class);
    cl.registerShortNameOfClass(Delimiter.class);
    cl.registerShortNameOfClass(MaxIterations.class);
  }

  /**
   * @return the configuration for driver.
   */
  public Configuration getDriverConfiguration() {
    return Tang.Factory.getTang().newConfigurationBuilder()
        .bindNamedParameter(
            NeuralNetworkTaskParameters.SerializedNeuralNetConf.class,
            serializedNeuralNetworkConfiguration)
        .bindNamedParameter(Delimiter.class, delimiter)
        .bindNamedParameter(MaxIterations.class, String.valueOf(maxIterations))
        .build();
  }
}
