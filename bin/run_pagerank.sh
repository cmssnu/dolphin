#!/bin/sh
# Copyright (C) 2015 Seoul National University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# EXAMPLE USAGE
# bin/run_pagerank.sh -convThr 0.01 -maxIter 10 -dampingFactor 0.85 -local true -split 1 -input dolphin-bsp/src/test/resources/data/pagerank -output output_pagerank -maxNumEvalLocal 2

SELF_JAR=`echo dolphin-bsp/target/dolphin-bsp-*-shaded.jar`

LOGGING_CONFIG='-Djava.util.logging.config.class=org.apache.reef.util.logging.Config'

CLASSPATH=$YARN_HOME/share/hadoop/common/*:$YARN_HOME/share/hadoop/common/lib/*:$YARN_HOME/share/hadoop/yarn/*:$YARN_HOME/share/hadoop/hdfs/*:$YARN_HOME/share/hadoop/mapreduce/lib/*:$YARN_HOME/share/hadoop/mapreduce/*

YARN_CONF_DIR=$YARN_HOME/etc/hadoop

ALG=edu.snu.dolphin.bsp.examples.ml.algorithms.graph.PageRankREEF

CMD="java -cp $YARN_CONF_DIR:$SELF_JAR:$CLASSPATH $LOGGING_CONFIG $ALG $*"
echo $CMD
$CMD
