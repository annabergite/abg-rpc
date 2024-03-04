package rpc.abg.serialization.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import rpc.abg.serialization.JsonMapper;

public class JacksonMapper implements JsonMapper {
	private static final ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();

		mapper.registerModule(new Jdk8Module());

		JavaTimeModule javaTimeModule = new JavaTimeModule();
		// Hack time module to allow 'Z' at the end of string (i.e. javascript json's)
		javaTimeModule.addDeserializer(LocalDateTime.class,
				new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
		mapper.registerModule(javaTimeModule);

		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		mapper.registerModule(new AfterburnerModule());
	}

	@Override
	public <T> T read(ByteBuf buffer, Class<T> type) throws IOException {
		try (InputStream inputStream = new ByteBufInputStream(buffer)) {
			return mapper.readValue(inputStream, type);
		}
	}

	@Override
	public void write(ByteBuf buffer, Object value) throws IOException {
		try (OutputStream outputStream = new ByteBufOutputStream(buffer)) {
			mapper.writeValue(outputStream, value);
		}
	}

}
