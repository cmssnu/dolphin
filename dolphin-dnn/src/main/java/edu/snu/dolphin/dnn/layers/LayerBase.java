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
package edu.snu.dolphin.dnn.layers;

import edu.snu.dolphin.dnn.blas.Matrix;

/**
 * Abstract class for the layer of a neural network.
 */
public abstract class LayerBase {

  private final int index;
  private final int numOutput;
  private LayerParameter layerParameter;

  protected LayerBase(final int index, final int numOutput) {
    this.index = index;
    this.numOutput = numOutput;
  }

  /**
   * @return the index of the layer.
   */
  public final int getIndex() {
    return this.index;
  }

  /**
   * @return the number of layer output nodes.
   */
  public final int getNumOutput() {
    return this.numOutput;
  }

  /**
   * Replaces the parameter of the layer.
   * @param layerParameter a new parameter of the layer.
   */
  public final void setLayerParameter(final LayerParameter layerParameter) {
    if (!isLearnable()) {
      throw new RuntimeException(this + " is not a learnable layer. setLayerParameter() should not be called.");
    }

    this.layerParameter = layerParameter;
  }

  /**
   * @return the parameter of the layer.
   */
  public final LayerParameter getLayerParameter() {
    if (!isLearnable()) {
      throw new RuntimeException(this + " is not a learnable layer. getLayerParameter() should not be called.");
    }

    return this.layerParameter;
  }

  /**
   * @return whether this layer can learn from training data or not.
   */
  public abstract boolean isLearnable();

  /**
   * Computes an output value.
   * @param input an input value for this layer.
   * @return an output value for this layer.
   */
  public abstract Matrix feedForward(final Matrix input);

  /**
   * Computes an error.
   * @param input the input value for this layer, or the expected output if the layer is a loss layer.
   * @param activation the activation value.
   * @param nextError an error of the next layer - the one closer to the output layer.
   * @return an error for this layer with the specified input value.
   */
  public abstract Matrix backPropagate(final Matrix input,
                                       final Matrix activation,
                                       final Matrix nextError);

  /**
   * Computes a parameter gradient for this layer.
   * @param input an input value for this layer.
   * @param error an error for this layer.
   * @return a parameter gradient for this layer or {@code null} if this layer is not learnable.
   */
  public abstract LayerParameter generateParameterGradient(final Matrix input, final Matrix error);
}
