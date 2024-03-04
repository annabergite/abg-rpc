package rpc.abg.util;

import io.netty.buffer.ByteBuf;

public class ByteBufUtils {

	public static void writeVarInt(ByteBuf byteBuf, int value) {
		if (value >>> 7 == 0) {
			byteBuf.writeByte((byte) value);
			return;
		}

		if (value >>> 14 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7);
			byteBuf.writeShort(newValue);

			return;
		}

		if (value >>> 21 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7 & 0xFF | 0x80);
			byteBuf.writeShort(newValue);
			byteBuf.writeByte((byte) (value >>> 14));

			return;
		}

		if (value >>> 28 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 24) //
					| ((value >>> 7 & 0xFF | 0x80) << 16) //
					| ((value >>> 14 & 0xFF | 0x80) << 8) //
					| (value >>> 21);

			byteBuf.writeInt(newValue);

			return;
		}

		int newValue = (((value & 0x7F) | 0x80) << 24) //
				| ((value >>> 7 & 0xFF | 0x80) << 16) //
				| ((value >>> 14 & 0xFF | 0x80) << 8) //
				| (value >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(newValue);
		byteBuf.writeByte((byte) (value >>> 28));
	}

	public static int readVarInt(ByteBuf byteBuf) {
		int b = byteBuf.readByte();
		int result = b & 0x7F;

		if ((b & 0x80) != 0) {
			b = byteBuf.readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuf.readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuf.readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuf.readByte();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}

		return result;
	}

	// 性能提升有限，一个byte的情况性能还会下降。不推荐使用
	public static int readVarIntByInt(ByteBuf byteBuf) {
		int readerIndex = byteBuf.readerIndex();
		int value = byteBuf.readIntLE();

		int b = value;
		int result = b & 0x7F;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 1);
			return result;
		}

		b = (value >> 8);
		result |= (b & 0x7F) << 7;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 2);
			return result;
		}

		b = (value >> 16);
		result |= (b & 0x7F) << 14;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 3);
			return result;
		}

		b = (value >>> 24);
		result |= (b & 0x7F) << 21;

		if ((b & 0x80) == 0) {
			return result;
		}

		b = byteBuf.readByte();
		result |= (b & 0x7F) << 28;

		return result;
	}

	// 性能提升有限，一个byte的情况性能还会下降。不推荐使用
	public static int readVarIntByLong(ByteBuf byteBuf) {
		int readerIndex = byteBuf.readerIndex();
		long value = byteBuf.readLongLE();

		int b = (int) value;
		int result = b & 0x7F;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 1);
			return result;
		}

		b = (int) (value >> 8);
		result |= (b & 0x7F) << 7;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 2);
			return result;
		}

		b = (int) (value >> 16);
		result |= (b & 0x7F) << 14;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 3);
			return result;
		}

		b = (int) (value >> 24);
		result |= (b & 0x7F) << 21;

		if ((b & 0x80) == 0) {
			byteBuf.readerIndex(readerIndex + 4);
			return result;
		}

		b = (int) (value >> 32);
		result |= (b & 0x7F) << 28;

		byteBuf.readerIndex(readerIndex + 5);
		return result;
	}

	public static void writeVarLong(ByteBuf byteBuf, long value) {
		if (value >>> 7 == 0) {
			byteBuf.writeByte((byte) value);
			return;
		}

		if (value >>> 14 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7);
			byteBuf.writeShort(newValue);

			return;
		}

		if (value >>> 21 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7 & 0xFF | 0x80);
			byteBuf.writeShort(newValue);
			byteBuf.writeByte((byte) (intValue >>> 14));

			return;
		}

		if (value >>> 28 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21);

			byteBuf.writeInt(newValue);

			return;
		}

		if (value >>> 35 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(newValue);
			byteBuf.writeByte((byte) (value >>> 28));

			return;
		}

		if (value >>> 42 == 0) {
			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8) //
					| (value >>> 35));

			byteBuf.writeShort(second);

			return;
		}

		if (value >>> 49 == 0) {

			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8) //
					| (value >>> 35 & 0xFF | 0x80));

			byteBuf.writeShort(second);
			byteBuf.writeByte((byte) (value >>> 42));

			return;
		}

		if (value >>> 56 == 0) {
			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			intValue = (int) (value >>> 28);
			int second = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21);

			byteBuf.writeInt(second);

			return;
		}

		int intValue = (int) value;
		int first = (((intValue & 0x7F) | 0x80) << 24) //
				| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
				| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
				| (intValue >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(first);

		intValue = (int) (value >>> 28);
		int second = (((intValue & 0x7F) | 0x80) << 24) //
				| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
				| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
				| (intValue >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(second);
		byteBuf.writeByte((byte) (value >>> 56));

		return;
	}

	public static long readVarLong(ByteBuf byteBuf) {
		int b = byteBuf.readByte();

		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = byteBuf.readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuf.readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuf.readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuf.readByte();
						result |= (long) (b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = byteBuf.readByte();
							result |= (long) (b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = byteBuf.readByte();
								result |= (long) (b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = byteBuf.readByte();
									result |= (long) (b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = byteBuf.readByte();
										result |= (long) b << 56;
									}
								}
							}
						}
					}
				}
			}
		}

		return result;
	}
}
