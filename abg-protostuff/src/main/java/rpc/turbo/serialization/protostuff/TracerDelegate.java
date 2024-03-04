package rpc.abg.serialization.protostuff;

import static io.protostuff.WireFormat.WIRETYPE_LENGTH_DELIMITED;
import static io.protostuff.WireFormat.makeTag;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.protostuff.ByteBufInput;
import io.protostuff.ByteBufOutput;
import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.WireFormat.FieldType;
import io.protostuff.runtime.Delegate;
import rpc.abg.serialization.TracerSerializer;
import rpc.abg.trace.Tracer;
import rpc.abg.util.ByteBufUtils;

public class TracerDelegate implements Delegate<Tracer> {
	private static final TracerSerializer tracerSerializer = new TracerSerializer();

	@Override
	public FieldType getFieldType() {
		return FieldType.BYTES;
	}

	@Override
	public Tracer readFrom(Input input) throws IOException {
		if (!(input instanceof ByteBufInput)) {
			throw new IOException("only support ByteBufInput");
		}

		ByteBuf byteBuf = ((ByteBufInput) input).getByteBuf();
		return tracerSerializer.read(byteBuf);
	}

	@Override
	public void writeTo(Output output, int number, Tracer tracer, boolean repeated) throws IOException {
		if (!(output instanceof ByteBufOutput)) {
			throw new IOException("only support ByteBufOutput");
		}

		ByteBuf byteBuf = ((ByteBufOutput) output).getByteBuf();
		ByteBufUtils.writeVarInt(byteBuf, makeTag(number, WIRETYPE_LENGTH_DELIMITED));
		tracerSerializer.write(byteBuf, tracer);
	}

	@Override
	public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
		throw new IOException("not support this method");
	}

	@Override
	public Class<?> typeClass() {
		return Tracer.class;
	}

}
