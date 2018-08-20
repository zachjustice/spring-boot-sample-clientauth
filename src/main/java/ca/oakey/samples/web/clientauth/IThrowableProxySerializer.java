package ca.oakey.samples.web.clientauth;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Created by danap on 6/13/17.
 */
public class IThrowableProxySerializer extends StdSerializer<IThrowableProxy> {

    public IThrowableProxySerializer() {
        super(IThrowableProxy.class);
    }

    @Override
    public void serialize(IThrowableProxy value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writeProxy(value, gen);
    }

    private void writeProxy(IThrowableProxy value, JsonGenerator gen) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("class", value.getClassName());
        gen.writeStringField("message", value.getMessage());
        gen.writeArrayFieldStart("stacktrace");
        for (StackTraceElementProxy element : value.getStackTraceElementProxyArray()) {
            gen.writeString(element.getSTEAsString());
        }
        gen.writeEndArray();
        if (value.getCause() != null) {
            gen.writeObjectField("cause", value.getCause());
        }
        gen.writeEndObject();
    }
}
