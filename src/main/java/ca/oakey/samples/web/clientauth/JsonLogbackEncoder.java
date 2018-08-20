package ca.oakey.samples.web.clientauth;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.function.Function;

/**
 * Created by danap on 4/28/17.
 */
public class JsonLogbackEncoder<E> extends EncoderBase<E> implements Encoder<E> {
    private static JsonFactory jsonFactory;

    static {
        SimpleModule throwableSerializationModule = new SimpleModule();
        throwableSerializationModule.addSerializer(Throwable.class, new ThrowableSerializer());
        throwableSerializationModule.addSerializer(IThrowableProxy.class, new IThrowableProxySerializer());
        throwableSerializationModule.addSerializer(Connection.class, new ToStringSerializer());
        jsonFactory = new JsonFactory(
            new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
                .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
                .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                .registerModule(throwableSerializationModule)
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .findAndRegisterModules());
    }

    public static JsonFactory getLoggingJsonFactory() {
        return jsonFactory;
    }

    private Function<E, ?> mapper;

    public JsonLogbackEncoder() {
    }

    public <R> Function<E, R> getMapper() {
        return (Function<E, R>) mapper;
    }

    public <R> void setMapper(Function<E, R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] headerBytes() {
        return new byte[0];
    }

    @Override
    public byte[] encode(E event) {
        StringWriter sw = new StringWriter();
        Object toLog = event;
        if (getMapper() != null) {
            toLog = getMapper().apply(event);
        }
        try {
            jsonFactory.createGenerator(sw).writeObject(toLog);
        } catch (IOException ex) {
            sw.flush();
            sw.append("\n*** Exception while writing to log: ").append(ex.getMessage()).append("\nStack Trace:\n");
            ex.printStackTrace(new PrintWriter(sw,true));
        }
        sw.append("\n");
        return sw.toString().getBytes();
    }

    @Override
    public byte[] footerBytes() {
        return new byte[0];
    }
}
