package rpc.abg.benchmark.unsafe;

import java.lang.reflect.Field;

import org.openjdk.jol.info.ClassLayout;

import rpc.abg.benchmark.bean.AllIntBean;
import rpc.abg.util.UnsafeUtils;

public class UnsafeTest {

	public static void main(String[] args) throws Exception {
		AllIntBean bean = new AllIntBean();

		bean.setValue0(0);
		bean.setValue1(1);
		bean.setValue2(2);
		bean.setValue3(3);
		bean.setValue4(4);
		bean.setValue5(5);
		bean.setValue6(6);
		bean.setValue7(7);
		bean.setValue8(8);
		bean.setValue9(9);
		bean.setValue10(10);
		bean.setValue11(11);
		bean.setValue12(12);
		bean.setValue13(13);
		bean.setValue14(14);
		bean.setValue15(15);
		bean.setValue16(16);
		bean.setValue17(17);
		bean.setValue18(18);
		bean.setValue19(19);
		bean.setValue20(20);
		bean.setValue21(21);
		bean.setValue22(22);
		bean.setValue23(23);
		bean.setValue24(24);
		bean.setValue25(25);
		bean.setValue26(26);
		bean.setValue27(27);
		bean.setValue28(28);
		bean.setValue29(29);
		bean.setValue30(30);
		bean.setValue31(31);
		
		System.out.println(ClassLayout.parseClass(Integer.class).toPrintable());
		System.out.println(ClassLayout.parseClass(AllIntBean.class).toPrintable());
		System.out.println(ClassLayout.parseInstance("hello").toPrintable());

		for (int i = 0; i < 32; i++) {
			Field valueField = AllIntBean.class.getDeclaredField("value" + i);
			long valueAddress = UnsafeUtils.unsafe().objectFieldOffset(valueField);
			System.out.printf("valueAddress%d:%d\r\n", i, valueAddress);
		}

		Field valueField0 = AllIntBean.class.getDeclaredField("value0");
		long valueAddress0 = UnsafeUtils.unsafe().objectFieldOffset(valueField0);

		Field valueField1 = AllIntBean.class.getDeclaredField("value1");
		long valueAddress1 = UnsafeUtils.unsafe().objectFieldOffset(valueField1);

		Field valueField31 = AllIntBean.class.getDeclaredField("value31");
		long valueAddress31 = UnsafeUtils.unsafe().objectFieldOffset(valueField31);

		System.out.println("valueAddress0:" + valueAddress0);
		System.out.println("valueAddress1:" + valueAddress1);
		System.out.println("valueAddress31:" + valueAddress31);

		AllIntBean bean2 = new AllIntBean();

		// IllegalArgumentException, only support array
		UnsafeUtils.unsafe().copyMemory(bean, valueAddress0, bean2, valueAddress0, 32 * 4);

		System.out.println(bean2.getValue0());
		System.out.println(bean2.getValue1());
		System.out.println(bean2.getValue30());
		System.out.println(bean2.getValue31());
	}

}
