package rpc.abg.transport.client.codec;

import io.netty.util.AttributeKey;

final class CodecConstants {

	final static AttributeKey<Boolean> STARTED_AUTO_EXPIRE_JOB = AttributeKey.valueOf("_STARTED_AUTO_EXPIRE_JOB_");
}
