package rpc.abg.serialization;

import io.netty.buffer.ByteBuf;
import rpc.abg.trace.Tracer;
import rpc.abg.util.ByteBufUtils;
import rpc.abg.util.uuid.ObjectId;

/**
 * Tracer 高效序列化实现
 * 
 * @author Annabergite
 *
 */
public final class TracerSerializer {

	private static final byte ENTITY_FLAG = 1;

	private static final byte TRACE_ID_FLAG = 1 << 1;
	private static final byte SPAN_ID_FLAG = 1 << 2;
	private static final byte PARENT_ID_FLAG = 1 << 3;

	private static final byte CS_FLAG = 1 << 4;
	private static final byte CR_FLAG = 1 << 5;
	private static final byte SS_FLAG = 1 << 6;
	private static final byte SR_FLAG = (byte) (1 << 7);

	public final void write(ByteBuf buffer, Tracer tracer) {
		if (tracer == null) {
			buffer.writeByte(0);
			return;
		}

		int flagIndex = buffer.writerIndex();
		buffer.writerIndex(flagIndex + 1);

		byte flag = ENTITY_FLAG;

		if (tracer.getTraceId() != null) {
			flag |= TRACE_ID_FLAG;
			tracer.getTraceId().writeTo(buffer);
		}

		if (tracer.getSpanId() != 0) {
			flag |= SPAN_ID_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getSpanId());
		}

		if (tracer.getParentId() != 0) {
			flag |= PARENT_ID_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getParentId());
		}

		if (tracer.getCs() != 0) {
			flag |= CS_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getCs());
		}

		if (tracer.getCr() != 0) {
			flag |= CR_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getCr());
		}

		if (tracer.getSs() != 0) {
			flag |= SS_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getSs());
		}

		if (tracer.getSr() != 0) {
			flag |= SR_FLAG;
			ByteBufUtils.writeVarLong(buffer, tracer.getSr());
		}

		buffer.setByte(flagIndex, flag);
	}

	public final Tracer read(ByteBuf buffer) {
		byte flag = buffer.readByte();

		if (flag == 0) {
			return null;
		}

		Tracer tracer = new Tracer();

		if ((flag & TRACE_ID_FLAG) == TRACE_ID_FLAG) {
			tracer.setTraceId(new ObjectId(buffer));
		}

		if ((flag & SPAN_ID_FLAG) == SPAN_ID_FLAG) {
			tracer.setSpanId(ByteBufUtils.readVarLong(buffer));
		}

		if ((flag & PARENT_ID_FLAG) == PARENT_ID_FLAG) {
			tracer.setParentId(ByteBufUtils.readVarLong(buffer));
		}

		if ((flag & CS_FLAG) == CS_FLAG) {
			tracer.setCs(ByteBufUtils.readVarLong(buffer));
		}

		if ((flag & CR_FLAG) == CR_FLAG) {
			tracer.setCr(ByteBufUtils.readVarLong(buffer));
		}

		if ((flag & SS_FLAG) == SS_FLAG) {
			tracer.setSs(ByteBufUtils.readVarLong(buffer));
		}

		if ((flag & SR_FLAG) == SR_FLAG) {
			tracer.setSr(ByteBufUtils.readVarLong(buffer));
		}

		return tracer;
	}

	public static void main(String[] args) {
		Tracer tracer = new Tracer();

		tracer.setTraceId(ObjectId.next());
		tracer.setSpanId(123);
		tracer.setParentId(456);

		tracer.setCs(System.currentTimeMillis());
		tracer.setCr(System.currentTimeMillis());
		tracer.setSs(System.currentTimeMillis());
		tracer.setSr(System.currentTimeMillis());

		TracerSerializer serializer = new TracerSerializer();
		ByteBuf buffer = io.netty.buffer.Unpooled.buffer(1024);

		serializer.write(buffer, tracer);
		Tracer tracer2 = serializer.read(buffer);

		System.out.println("length: " + buffer.writerIndex());
		System.out.println(tracer2);
	}
}
