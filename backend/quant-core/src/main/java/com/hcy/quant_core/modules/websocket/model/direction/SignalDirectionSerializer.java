package com.hcy.quant_core.modules.websocket.model.direction;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class SignalDirectionSerializer extends StdSerializer<SignalDirection> {

	public SignalDirectionSerializer() {
		super(SignalDirection.class);
	}

	@Override
	public void serialize(SignalDirection value, JsonGenerator gen,
		SerializerProvider provider) throws IOException {
		gen.writeString(switch (value) {
			case StatArbDirection d -> d.name();
			case OnChainDirection d -> d.name();
		});
	}
}
