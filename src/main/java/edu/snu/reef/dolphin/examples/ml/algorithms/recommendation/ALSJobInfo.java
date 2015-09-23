/**
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
package edu.snu.reef.dolphin.examples.ml.algorithms.recommendation;

import edu.snu.reef.dolphin.core.DataParser;
import edu.snu.reef.dolphin.core.StageInfo;
import edu.snu.reef.dolphin.core.UserJobInfo;
import edu.snu.reef.dolphin.examples.ml.data.RecommendationDataParser;
import edu.snu.reef.dolphin.examples.ml.sub.ALSSummaryCodec;
import edu.snu.reef.dolphin.examples.ml.sub.MapOfIntVecCodec;
import edu.snu.reef.dolphin.examples.ml.sub.MapOfIntVecReduceFunction;
import edu.snu.reef.dolphin.examples.ml.sub.RatingListCodec;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public final class ALSJobInfo implements UserJobInfo {

  @Inject
  public ALSJobInfo() {
  }

  @Override
  public List<StageInfo> getStageInfoList() {
    final List<StageInfo> stageInfoList = new LinkedList<>();

    stageInfoList.add(
        StageInfo.newBuilder(RecommendationGatherPreCmpTask.class,
                             RecommendationGatherPreCtrlTask.class,
                             ALSGatherPreCommGroup.class)
            .setGather(RatingListCodec.class)
            .build());

    stageInfoList.add(
        StageInfo.newBuilder(RecommendationScatterUserPreCmpTask.class,
                             RecommendationScatterUserPreCtrlTask.class,
                             ALSScatterUserPreCommGroup.class)
            .setScatter(RatingListCodec.class)
            .build());

    stageInfoList.add(
        StageInfo.newBuilder(RecommendationScatterItemPreCmpTask.class,
                             RecommendationScatterItemPreCtrlTask.class,
                             ALSScatterItemPreCommGroup.class)
            .setScatter(RatingListCodec.class)
            .build());

    stageInfoList.add(
        StageInfo.newBuilder(ALSCmpTask.class,
                             ALSCtrlTask.class,
                             ALSCommGroup.class)
            .setBroadcast(ALSSummaryCodec.class)
            .setReduce(MapOfIntVecCodec.class, MapOfIntVecReduceFunction.class)
            .build());

    return stageInfoList;
  }

  @Override
  public Class<? extends DataParser> getDataParser() {
    return RecommendationDataParser.class;
  }
}
