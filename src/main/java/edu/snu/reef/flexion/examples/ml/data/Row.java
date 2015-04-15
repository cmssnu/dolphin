/**
 * Copyright (C) 2014 Seoul National University
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
package edu.snu.reef.flexion.examples.ml.data;

import org.apache.mahout.math.Vector;

public final class Row {
  private final double output;
  private final Vector feature;

  public Row(final double output, final Vector feature) {
    this.output = output;
    this.feature = feature;
  }

  public final double getOutput() {
    return output;
  }

  public final Vector getFeature() {
    return feature;
  }
}