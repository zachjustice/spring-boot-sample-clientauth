package ca.oakey.samples.web.clientauth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Created by danap on 6/13/17.
 */
public class ThrowableSerializer extends StdSerializer<Throwable> {

    public ThrowableSerializer() {
        super(Throwable.class);
    }

    private void writeThrowableFields(Throwable value, JsonGenerator gen, StackTraceElement lowestSoFar)
        throws IOException {
        gen.writeStartObject();
        try {
            gen.writeStringField("class", value.getClass().getName());
            gen.writeStringField("message", value.getMessage());
            gen.writeArrayFieldStart("stacktrace");
            try {
                for (StackTraceElement element : value.getStackTrace()) {
                    gen.writeString(element.toString());
                    if (lowestSoFar != null
                        && element.getClassName().equals(lowestSoFar.getClassName())
                        && element.getMethodName().equals(lowestSoFar.getMethodName())) {
                        break;
                    }
                }
            } finally {
                gen.writeEndArray();
            }
            if (value.getCause() != null) {
                gen.writeFieldName("cause");
                writeThrowableFields(value.getCause(), gen, value.getStackTrace()[0]);
            }
        } finally {
            gen.writeEndObject();
        }
    }

    @Override
    public void serialize(Throwable value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writeThrowableFields(value, gen, null);
    }
}
