package edu.snu.reef.flexion.examples.ml.algorithms.clustering.em;


import edu.snu.reef.flexion.core.DataParser;
import edu.snu.reef.flexion.core.StageInfo;
import edu.snu.reef.flexion.core.UserJobInfo;
import edu.snu.reef.flexion.examples.ml.algorithms.clustering.ClusteringPreStageBuilder;
import edu.snu.reef.flexion.examples.ml.data.ClusteringDataParser;
import edu.snu.reef.flexion.examples.ml.sub.ClusterSummaryListCodec;
import edu.snu.reef.flexion.examples.ml.sub.MapOfIntClusterStatsCodec;
import edu.snu.reef.flexion.examples.ml.sub.MapOfIntClusterStatsReduceFunction;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public final class EMJobInfo implements UserJobInfo {

  @Inject
  public EMJobInfo(){
  }

  @Override
  public List<StageInfo> getStageInfoList() {

    final List<StageInfo> stageInfoList = new LinkedList<>();

    // preprocess: initialize the centroids of clusters
    stageInfoList.add(ClusteringPreStageBuilder.build());

    // main process: adjust the centroids and covariance matrices of clusters
    stageInfoList.add(
        StageInfo.newBuilder(EMMainCmpTask.class, EMMainCtrlTask.class, EMMainCommGroup.class)
            .setBroadcast(ClusterSummaryListCodec.class)
            .setReduce(MapOfIntClusterStatsCodec.class, MapOfIntClusterStatsReduceFunction.class)
            .build());

    return stageInfoList;
  }

  @Override
  public Class<? extends DataParser> getDataParser() {
    return ClusteringDataParser.class;
  }
}