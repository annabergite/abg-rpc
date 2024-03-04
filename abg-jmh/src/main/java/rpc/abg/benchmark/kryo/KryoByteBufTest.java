package rpc.abg.benchmark.kryo;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.serialization.kryo.ByteBufInput;
import rpc.abg.serialization.kryo.ByteBufOutput;

public class KryoByteBufTest {
	public static class IntObj {
		public int i = Integer.MAX_VALUE;
	}

	public static class ManyIntObj {
		public int A = Integer.MAX_VALUE;
		public int a = Integer.MAX_VALUE;
		public int B = Integer.MAX_VALUE;
		public int b = Integer.MAX_VALUE;
		public int C = Integer.MAX_VALUE;
		public int c = Integer.MAX_VALUE;
		public int D = Integer.MAX_VALUE;
		public int d = Integer.MAX_VALUE;
		public int E = Integer.MAX_VALUE;
		public int e = Integer.MAX_VALUE;
		public int F = Integer.MAX_VALUE;
		public int f = Integer.MAX_VALUE;
		public int G = Integer.MAX_VALUE;
		public int g = Integer.MAX_VALUE;
		public int H = Integer.MAX_VALUE;
		public int h = Integer.MAX_VALUE;
		public int I = Integer.MAX_VALUE;
		public int i = Integer.MAX_VALUE;
		public int J = Integer.MAX_VALUE;
		public int j = Integer.MAX_VALUE;
		public int K = Integer.MAX_VALUE;
		public int k = Integer.MAX_VALUE;
		public int L = Integer.MAX_VALUE;
		public int l = Integer.MAX_VALUE;
		public int M = Integer.MAX_VALUE;
		public int m = Integer.MAX_VALUE;
		public int N = Integer.MAX_VALUE;
		public int n = Integer.MAX_VALUE;
		public int O = Integer.MAX_VALUE;
		public int o = Integer.MAX_VALUE;
		public int P = Integer.MAX_VALUE;
		public int p = Integer.MAX_VALUE;
		public int Q = Integer.MAX_VALUE;
		public int q = Integer.MAX_VALUE;
		public int R = Integer.MAX_VALUE;
		public int r = Integer.MAX_VALUE;
		public int S = Integer.MAX_VALUE;
		public int s = Integer.MAX_VALUE;
		public int T = Integer.MAX_VALUE;
		public int t = Integer.MAX_VALUE;
		public int U = Integer.MAX_VALUE;
		public int u = Integer.MAX_VALUE;
		public int V = Integer.MAX_VALUE;
		public int v = Integer.MAX_VALUE;
		public int W = Integer.MAX_VALUE;
		public int w = Integer.MAX_VALUE;
		public int X = Integer.MAX_VALUE;
		public int x = Integer.MAX_VALUE;
		public int Y = Integer.MAX_VALUE;
		public int y = Integer.MAX_VALUE;
		public int Z = Integer.MAX_VALUE;
		public int z = Integer.MAX_VALUE;

	}

	public static class IntLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameLongNameObj {
		public int i = Integer.MAX_VALUE;
	}

	public static class StringASCIIObj {
		public String str = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz";
	}

	public static class StringUTF8Obj {
		public String str = "中国 北京 中关村 中关村创业大街 110号 鼎好大厦 B1256";
	}

	public static class LocalDateTimeObj {
		public LocalDateTime dateTime = LocalDateTime.now();
	}

	public static class IntArrayObj {
		public int[] list = new int[32];

		public IntArrayObj() {
			for (int i = 0; i < 32; i++) {
				list[i] = i;
			}
		}
	}

	public static class ArrayListObj {
		public ArrayList<Integer> list = new ArrayList<>();

		public ArrayListObj() {
			for (int i = 0; i < 32; i++) {
				list.add(i);
			}
		}
	}

	public static class ComplexObj {
		public IntObj IntObj = new IntObj();
		public LocalDateTimeObj LocalDateTimeObj = new LocalDateTimeObj();
		public StringUTF8Obj StringUTF8Obj = new StringUTF8Obj();
		public StringASCIIObj StringASCIIObj = new StringASCIIObj();
		public ArrayListObj ArrayListObj = new ArrayListObj();
	}

	public static void main(String[] args) {
		Kryo kryo = new Kryo();
		// kryo.setWarnUnregisteredClasses(true);
		// kryo.setReferences(false);

		kryo.register(IntObj.class);
		kryo.register(StringUTF8Obj.class);
		kryo.register(LocalDateTimeObj.class);
		kryo.register(ComplexObj.class);
		kryo.register(int[].class);
		kryo.register(ArrayList.class);
		kryo.register(ArrayListObj.class);

		ByteBufAllocator allocator = new PooledByteBufAllocator(true);
		ByteBuf buffer = allocator.directBuffer(2, 1024 * 1024 * 8);
		System.out.println("buffer.nioBufferCount: " + buffer.nioBufferCount());
		System.out.println(buffer.nioBuffer(0, buffer.capacity()));

		ByteBufOutput output = new ByteBufOutput(buffer);

		UserService userService = new UserServiceServerImpl();
		User user = userService.getUser(Long.MAX_VALUE).join();
		
		while(true) {
			buffer.clear();
			output.setBuffer(buffer);
			kryo.writeObject(output, user);

			ByteBufInput input = new ByteBufInput(buffer);
			User u = kryo.readObject(input, User.class);
			System.out.println(u);
		}

//		buffer.clear();
//		output.setBuffer(buffer);
//		kryo.writeObject(output, user);
//		System.out.println("user writeObject: " + output.total());
//
//		ByteBufInput input = new ByteBufInput(buffer);
//		System.out.println("user readableBytes: " + buffer.readableBytes());
//		System.out.println("user readerIndex: " + buffer.readerIndex());
//		System.out.println(kryo.readObject(input, User.class));
	}
}
