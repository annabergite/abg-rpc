package rpc.abg.test;

import java.io.IOException;
import java.lang.reflect.Method;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.param.MethodParamClassFactory;

public class MethodParamClassFactoryTest {

	public static void main(String[] args) throws CannotCompileException, NotFoundException, IOException {
		CtClass.debugDump = "/tmp/debugDump";

		for (Method method : UserService.class.getMethods()) {
			MethodParamClassFactory.createClass(method);
		}
	}
}
