/*
 * Copyright (C) 2016 Seoul National University
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
package edu.snu.dolphin.dnn.layerparam.initializer;

import edu.snu.dolphin.dnn.blas.MatrixFactory;
import edu.snu.dolphin.dnn.conf.LayerConfigurationParameters;
import edu.snu.dolphin.dnn.layers.LayerParameter;
import org.apache.reef.tang.annotations.Parameter;

import javax.inject.Inject;

import static edu.snu.dolphin.dnn.util.NeuralNetworkUtils.shapeFromString;

/**
 * Pooling Layer parameter initializer.
 *
 * This initializer is for pooling layer which do not have layer parameter.
 */
public final class PoolingLayerParameterInitializer implements LayerParameterInitializer {

  private final int index;
  private final int[] inputShape;
  private final int strideHeight;
  private final int strideWidth;
  private final int kernelHeight;
  private final int kernelWidth;
  private final LayerParameter emptyLayerParam;

  @Inject
  public PoolingLayerParameterInitializer(
      final MatrixFactory matrixFactory,
      @Parameter(LayerConfigurationParameters.LayerIndex.class) final int index,
      @Parameter(LayerConfigurationParameters.LayerInputShape.class) final String inputShape,
      @Parameter(LayerConfigurationParameters.StrideHeight.class) final int strideHeight,
      @Parameter(LayerConfigurationParameters.StrideWidth.class) final int strideWidth,
      @Parameter(LayerConfigurationParameters.KernelHeight.class) final int kernelHeight,
      @Parameter(LayerConfigurationParameters.KernelWidth.class) final int kernelWidth) {
    this.index = index;
    this.inputShape = shapeFromString(inputShape);
    this.strideHeight = strideHeight;
    this.strideWidth = strideWidth;
    this.kernelHeight = kernelHeight;
    this.kernelWidth = kernelWidth;
    this.emptyLayerParam = LayerParameter.newEmptyInstance(matrixFactory);
  }

  /**
   * @return the initial parameter of the layer.
   */
  public LayerParameter generateInitialParameter() {
    return emptyLayerParam;
  }

  /**
   * @return the index of the layer.
   */
  public int getIndex() {
    return index;
  }

  /**
   * This function computes output shape.
   * input shape: row * col
   * output shape: row' * col'
   * row = (row − kernelHeight) / stride + 1
   * col = (col − kernelWidth) / stride + 1
   */
  @Override
  public int[] getOutputShape() {
    final int[] computedShape;
    switch (inputShape.length) {
    case 1 :
      computedShape = new int[1];
      computedShape[0] = (inputShape[0] - kernelHeight) / strideHeight + 1;
      return computedShape;
    case 2 :
      computedShape = new int[2];
      computedShape[0] = (inputShape[0] - kernelHeight) / strideHeight + 1;
      computedShape[1] = (inputShape[1] - kernelWidth) / strideWidth + 1;
      return computedShape;
    default :
      throw new IllegalArgumentException("Unsupported input dimension: " + Integer.toString(inputShape.length));
    }
  }
}