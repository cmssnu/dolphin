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
package edu.snu.dolphin.bsp.examples.ml.sub;

import edu.snu.dolphin.bsp.examples.ml.data.PageRankSummary;
import org.apache.reef.io.serialization.Codec;

import javax.inject.Inject;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PageRankSummaryCodec implements Codec<PageRankSummary> {

  @Inject
  public PageRankSummaryCodec() {
  }

  @Override
  public byte[] encode(final PageRankSummary summary) {
    final Map<Integer, Double> model = summary.getModel();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(Integer.SIZE // count
        + (Integer.SIZE + Double.SIZE) * model.size());

    try (final DataOutputStream daos = new DataOutputStream(baos)) {
      daos.writeInt(model.size());

      for (final Map.Entry<Integer, Double> entry : model.entrySet()) {
        daos.writeInt(entry.getKey());
        daos.writeDouble(entry.getValue());
      }
    } catch (final IOException e) {
      throw new RuntimeException(e.getCause());
    }

    return baos.toByteArray();
  }

  @Override
  public PageRankSummary decode(final byte[] data) {
    final ByteArrayInputStream bais = new ByteArrayInputStream(data);
    final Map<Integer, Double> model = new HashMap<>();

    try (final DataInputStream dais = new DataInputStream(bais)) {
      final int count = dais.readInt();
      for (int i = 0; i < count; i++) {
        final Integer nodeId = dais.readInt();
        final Double value = dais.readDouble();
        model.put(nodeId, value);
      }
    } catch (final IOException e) {
      throw new RuntimeException(e.getCause());
    }
    return new PageRankSummary(model);
  }

}
