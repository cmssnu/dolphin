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
package edu.snu.dolphin.dnn.blas.jblas;

import edu.snu.dolphin.dnn.blas.Matrix;
import edu.snu.dolphin.dnn.blas.MatrixFactory;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;
import org.jblas.FloatMatrix;

import javax.inject.Inject;

/**
 * Factory class for JBLAS based matrix implementation.
 */
public final class MatrixJBLASFactory implements MatrixFactory {

  private static final SynchronizedRandomGenerator RANDOM = new SynchronizedRandomGenerator(new MersenneTwister());

  @Inject
  private MatrixJBLASFactory() {
  }

  @Override
  public Matrix create(final int length) {
    return new MatrixJBLASImpl(new FloatMatrix(1, length));
  }

  @Override
  public Matrix create(final int rows, final int columns) {
    return new MatrixJBLASImpl(new FloatMatrix(rows, columns));
  }

  @Override
  public Matrix create(final float[] data) {
    return new MatrixJBLASImpl(new FloatMatrix(1, data.length, data));
  }

  @Override
  public Matrix create(final float[][] data) {
    return new MatrixJBLASImpl(new FloatMatrix(data));
  }

  @Override
  public Matrix create(final float[] data, final int rows, final int columns) {
    return new MatrixJBLASImpl(new FloatMatrix(rows, columns, data));
  }

  @Override
  public Matrix ones(final int length) {
    return new MatrixJBLASImpl(FloatMatrix.ones(1, length));
  }

  @Override
  public Matrix ones(final int rows, final int columns) {
    return new MatrixJBLASImpl(FloatMatrix.ones(rows, columns));
  }

  @Override
  public Matrix zeros(final int length) {
    return new MatrixJBLASImpl(FloatMatrix.zeros(1, length));
  }

  @Override
  public Matrix zeros(final int rows, final int columns) {
    return new MatrixJBLASImpl(FloatMatrix.zeros(rows, columns));
  }

  @Override
  public Matrix rand(final int length) {
    return rand(1, length);
  }

  @Override
  public Matrix rand(final int rows, final int columns) {
    final int length = rows * columns;
    final float[] data = new float[length];

    for (int i = 0; i < length; ++i) {
      data[i] = RANDOM.nextFloat();
    }

    return new MatrixJBLASImpl(new FloatMatrix(rows, columns, data));
  }

  @Override
  public Matrix rand(final int rows, final int columns, final long seed) {
    RANDOM.setSeed(seed);
    return rand(rows, columns);
  }

  @Override
  public Matrix randn(final int length) {
    return randn(1, length);
  }

  @Override
  public Matrix randn(final int rows, final int columns) {
    final int length = rows * columns;
    final float[] data = new float[length];

    for (int i = 0; i < length; ++i) {
      data[i] = (float) RANDOM.nextGaussian();
    }

    return new MatrixJBLASImpl(new FloatMatrix(rows, columns, data));
  }

  @Override
  public Matrix randn(final int rows, final int columns, final long seed) {
    RANDOM.setSeed(seed);
    return randn(rows, columns);
  }

  @Override
  public Matrix concatHorizontally(final Matrix a, final Matrix b) {
    if (a instanceof MatrixJBLASImpl && b instanceof MatrixJBLASImpl) {
      return MatrixJBLASImpl.concatHorizontally((MatrixJBLASImpl) a, (MatrixJBLASImpl) b);
    } else {
      throw new IllegalArgumentException("Matrices for the concatenation should be JBLAS based");
    }
  }

  @Override
  public Matrix concatVertically(final Matrix a, final Matrix b) {
    if (a instanceof MatrixJBLASImpl && b instanceof MatrixJBLASImpl) {
      return MatrixJBLASImpl.concatVertically((MatrixJBLASImpl) a, (MatrixJBLASImpl) b);
    } else {
      throw new IllegalArgumentException("Matrices for the concatenation should be JBLAS based");
    }
  }
}