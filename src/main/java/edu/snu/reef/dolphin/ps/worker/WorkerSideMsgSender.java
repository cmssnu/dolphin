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
package edu.snu.reef.dolphin.ps.worker;

import edu.snu.reef.dolphin.ps.ParameterServerConfiguration.KeyCodecName;
import edu.snu.reef.dolphin.ps.ParameterServerConfiguration.PreValueCodecName;
import edu.snu.reef.dolphin.ps.avro.AvroParameterServerMsg;
import edu.snu.reef.dolphin.ps.avro.PushMsg;
import edu.snu.reef.dolphin.ps.avro.PullMsg;
import edu.snu.reef.dolphin.ps.avro.Type;
import edu.snu.reef.dolphin.ps.ns.PSNetworkSetup;
import org.apache.reef.annotations.audience.EvaluatorSide;
import org.apache.reef.exception.evaluator.NetworkException;
import org.apache.reef.io.network.Connection;
import org.apache.reef.io.serialization.Codec;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.wake.IdentifierFactory;

import javax.inject.Inject;
import java.nio.ByteBuffer;

/**
 * Worker-side Parameter Server message sender.
 */
@EvaluatorSide
public final class WorkerSideMsgSender<K, P> {

  /**
   * Network Connection Service related setup required for a Parameter Server application.
   */
  private final PSNetworkSetup psNetworkSetup;

  /**
   * Required for using Network Connection Service API.
   */
  private final IdentifierFactory identifierFactory;

  /**
   * Codec for encoding PS keys.
   */
  private final Codec<K> keyCodec;

  /**
   * Codec for encoding PS preValues.
   */
  private final Codec<P> preValueCodec;

  @Inject
  private WorkerSideMsgSender(final PSNetworkSetup psNetworkSetup,
                              final IdentifierFactory identifierFactory,
                              @Parameter(KeyCodecName.class) final Codec<K> keyCodec,
                              @Parameter(PreValueCodecName.class) final Codec<P> preValueCodec) {
    this.psNetworkSetup = psNetworkSetup;
    this.identifierFactory = identifierFactory;    
    this.keyCodec = keyCodec;
    this.preValueCodec = preValueCodec;
  }

  private void send(final String destId, final AvroParameterServerMsg msg) {
    final Connection<AvroParameterServerMsg> conn = psNetworkSetup.getConnectionFactory()
        .newConnection(identifierFactory.getNewInstance(destId));
    try {
      conn.open();
      conn.write(msg);
    } catch (final NetworkException ex) {
      throw new RuntimeException("NetworkException during connection open/write", ex);
    }
  }

  /**
   * Send a key-value pair to another evaluator.
   * @param destId Network Connection Service identifier of the destination evaluator
   * @param key key object representing what is being sent
   * @param preValue value to be sent to the destination
   */
  public void sendPushMsg(final String destId, final K key, final P preValue) {
    final PushMsg pushMsg = PushMsg.newBuilder()
        .setKey(ByteBuffer.wrap(keyCodec.encode(key)))
        .setPreValue(ByteBuffer.wrap(preValueCodec.encode(preValue)))
        .build();

    send(destId,
        AvroParameterServerMsg.newBuilder()
            .setType(Type.PushMsg)
            .setPushMsg(pushMsg)
            .build());
  }

  /**
   * Send a request to another evaluator for fetching a certain value.
   * After this message, a {@link edu.snu.reef.dolphin.ps.avro.ReplyMsg} containing the requested value
   * should be sent from the destination as a reply.
   * @param destId Network Connection Service identifier of the destination evaluator
   * @param key key object representing the expected value
   */
  public void sendPullMsg(final String destId, final K key) {
    final PullMsg pullMsg = PullMsg.newBuilder()
        .setKey(ByteBuffer.wrap(keyCodec.encode(key)))
        .setSrcId(psNetworkSetup.getMyId().toString())
        .build();

    send(destId,
        AvroParameterServerMsg.newBuilder()
            .setType(Type.PullMsg)
            .setPullMsg(pullMsg)
            .build());
  }
}