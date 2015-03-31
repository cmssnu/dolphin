package edu.snu.reef.flexion.examples.ml.algorithms.kmeans;

import edu.snu.reef.flexion.core.FlexionConfiguration;
import edu.snu.reef.flexion.core.FlexionLauncher;

public class KMeansREEF {

    public final static void main(String[] args) throws Exception {

        FlexionLauncher.run(FlexionConfiguration.CONF(args, KMeansParameters.getCommandLine())
                .set(FlexionConfiguration.IDENTIFIER, "K-means Clustering")
                .set(FlexionConfiguration.JOB_INFO, KMeansJobInfo.class)
                .set(FlexionConfiguration.PARAMETERS, KMeansParameters.class)
                .build());
    }


}