/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package rpc.abg.serialization.kryo;

import static com.esotericsoftware.kryo.util.Util.className;
import static com.esotericsoftware.kryo.util.Util.getWrapperClass;
import static com.esotericsoftware.kryo.util.Util.log;
import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.WARN;
import static com.esotericsoftware.minlog.Log.trace;
import static com.esotericsoftware.minlog.Log.warn;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IdentityObjectIntMap;

import rpc.abg.util.FastMap;
import rpc.abg.util.IntToObjectArrayMap;

/**
 * Base on kryo DefaultClassResolver. Resolves classes by ID or by fully
 * qualified class name.
 * 
 * @author Nathan Sweet
 */
@SuppressWarnings("rawtypes")
public class FastClassResolver implements ClassResolver {

	static public final byte NAME = -1;

	protected Kryo kryo;

	protected final IntToObjectArrayMap<Registration> idToRegistration = new IntToObjectArrayMap<>();
	protected final FastMap<Class, Registration> classToRegistration = new FastMap<>(32, 0.5F);

	protected IdentityObjectIntMap<Class> classToNameId;
	protected IntToObjectArrayMap<Class> nameIdToClass;
	protected FastMap<String, Class> nameToClass;
	protected int nextNameId;

	private int memoizedClassId = -1;
	private Registration memoizedClassIdValue;
	private Class memoizedClass;
	private Registration memoizedClassValue;

	public void setKryo(Kryo kryo) {
		this.kryo = kryo;
	}

	public Registration register(Registration registration) {
		if (registration == null)
			throw new IllegalArgumentException("registration cannot be null.");
		if (registration.getId() != NAME) {
			if (TRACE) {
				trace("kryo", "Register class ID " + registration.getId() + ": " + className(registration.getType())
						+ " (" + registration.getSerializer().getClass().getName() + ")");
			}
			idToRegistration.put(registration.getId(), registration);
		} else if (TRACE) {
			trace("kryo", "Register class name: " + className(registration.getType()) + " ("
					+ registration.getSerializer().getClass().getName() + ")");
		}
		classToRegistration.put(registration.getType(), registration);
		if (registration.getType().isPrimitive())
			classToRegistration.put(getWrapperClass(registration.getType()), registration);
		return registration;
	}

	public Registration registerImplicit(Class type) {
		return register(new Registration(type, kryo.getDefaultSerializer(type), NAME));
	}

	public Registration getRegistration(Class type) {
		if (type == memoizedClass)
			return memoizedClassValue;
		Registration registration = classToRegistration.get(type);
		if (registration != null) {
			memoizedClass = type;
			memoizedClassValue = registration;
		}
		return registration;
	}

	public Registration getRegistration(int classID) {
		return idToRegistration.get(classID);
	}

	public Registration writeClass(Output output, Class type) {
		if (type == null) {
			if (TRACE || (DEBUG && kryo.getDepth() == 1))
				log("Write", null);
			output.writeVarInt(Kryo.NULL, true);
			return null;
		}
		Registration registration = kryo.getRegistration(type);
		if (registration.getId() == NAME)
			writeName(output, type, registration);
		else {
			if (TRACE)
				trace("kryo", "Write class " + registration.getId() + ": " + className(type));
			output.writeVarInt(registration.getId() + 2, true);
		}
		return registration;
	}

	public void writeName(Output output, Class type, Registration registration) {
		output.writeVarInt(NAME + 2, true);
		if (classToNameId != null) {
			int nameId = classToNameId.get(type, -1);
			if (nameId != -1) {
				if (TRACE)
					trace("kryo", "Write class name reference " + nameId + ": " + className(type));
				output.writeVarInt(nameId, true);
				return;
			}
		}
		// Only write the class name the first time encountered in object graph.
		if (TRACE)
			trace("kryo", "Write class name: " + className(type));
		int nameId = nextNameId++;
		if (classToNameId == null)
			classToNameId = new IdentityObjectIntMap<>(32, 0.5F);
		classToNameId.put(type, nameId);
		output.writeVarInt(nameId, true);
		output.writeString(type.getName());
	}

	public Registration readClass(Input input) {
		int classID = input.readVarInt(true);
		switch (classID) {
		case Kryo.NULL:
			if (TRACE || (DEBUG && kryo.getDepth() == 1))
				log("Read", null);
			return null;
		case NAME + 2: // Offset for NAME and NULL.
			return readName(input);
		}
		if (classID == memoizedClassId)
			return memoizedClassIdValue;
		Registration registration = idToRegistration.get(classID - 2);
		if (registration == null)
			throw new KryoException("Encountered unregistered class ID: " + (classID - 2));
		if (TRACE)
			trace("kryo", "Read class " + (classID - 2) + ": " + className(registration.getType()));
		memoizedClassId = classID;
		memoizedClassIdValue = registration;
		return registration;
	}

	protected Registration readName(Input input) {
		int nameId = input.readVarInt(true);
		if (nameIdToClass == null)
			nameIdToClass = new IntToObjectArrayMap<>();
		Class type = nameIdToClass.get(nameId);
		if (type == null) {
			// Only read the class name the first time encountered in object graph.
			String className = input.readString();
			type = getTypeByName(className);
			if (type == null) {
				try {
					type = Class.forName(className, false, kryo.getClassLoader());
				} catch (ClassNotFoundException ex) {
					if (WARN)
						warn("kryo", "Unable to load class " + className
								+ " with kryo's ClassLoader. Retrying with current..");
					try {
						type = Class.forName(className);
					} catch (ClassNotFoundException e) {
						throw new KryoException("Unable to find class: " + className, ex);
					}
				}
				if (nameToClass == null)
					nameToClass = new FastMap<>(32, 0.5F);
				nameToClass.put(className, type);
			}
			nameIdToClass.put(nameId, type);
			if (TRACE)
				trace("kryo", "Read class name: " + className);
		} else {
			if (TRACE)
				trace("kryo", "Read class name reference " + nameId + ": " + className(type));
		}
		return kryo.getRegistration(type);
	}

	protected Class<?> getTypeByName(final String className) {
		return nameToClass != null ? nameToClass.get(className) : null;
	}

	public void reset() {
		if (!kryo.isRegistrationRequired()) {
			if (classToNameId != null)
				classToNameId.clear();
			if (nameIdToClass != null)
				nameIdToClass.clear();
			nextNameId = 0;
		}
	}

}
