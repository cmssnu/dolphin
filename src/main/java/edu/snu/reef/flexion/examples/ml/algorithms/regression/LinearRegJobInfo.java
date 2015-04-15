package edu.snu.reef.flexion.examples.ml.algorithms.regression;


import edu.snu.reef.flexion.core.DataParser;
import edu.snu.reef.flexion.core.StageInfo;
import edu.snu.reef.flexion.core.UserJobInfo;
import edu.snu.reef.flexion.examples.ml.data.RegressionDataParser;
import edu.snu.reef.flexion.examples.ml.parameters.CommunicationGroup;
import edu.snu.reef.flexion.examples.ml.sub.LinearModelCodec;
import edu.snu.reef.flexion.examples.ml.sub.LinearRegReduceFunction;
import edu.snu.reef.flexion.examples.ml.sub.LinearRegSummaryCodec;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public final class LinearRegJobInfo implements UserJobInfo {

  @Inject
  public LinearRegJobInfo(){
  }

  @Override
  public List<StageInfo> getStageInfoList() {

    final List<StageInfo> stageInfoList = new LinkedList<>();

    stageInfoList.add(
        StageInfo.newBuilder(LinearRegCmpTask.class, LinearRegCtrlTask.class, CommunicationGroup.class)
            .setBroadcast(LinearModelCodec.class)
            .setReduce(LinearRegSummaryCodec.class, LinearRegReduceFunction.class)
            .build());

    return stageInfoList;
  }

  @Override
  public Class<? extends DataParser> getDataParser() {
    return RegressionDataParser.class;
  }
}