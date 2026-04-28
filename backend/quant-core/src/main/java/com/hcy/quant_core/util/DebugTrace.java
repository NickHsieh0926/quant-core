package com.hcy.quant_core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.Generated;
import org.slf4j.Logger;

@Generated("jacoco.code.ignore")
public final class DebugTrace {
  private final Logger logger;
  private final boolean isDebugEnabled;
  private static final ObjectMapper MAPPER =
      JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  public DebugTrace(Logger logger, boolean isDebugEnabled) {
    this.logger = logger;
    this.isDebugEnabled = isDebugEnabled;
  }

  public void message(String format, Object... arguments) {
    if (this.isDebugEnabled) {
      logger.debug(format, arguments);
    }
  }

  public String toDebugJson4Obj(Object object) {
    if (isDebugEnabled) {
      try {
        return MAPPER.writeValueAsString(object);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("toDebugJson fail", e);
      }
    } else {
      return "N/A in non-debug-level";
    }
  }

  public static String toDebugJson(Object object) {
    try {
      return MAPPER.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("toDebugJson fail", e);
    }
  }

  public static void traceObjectNull(Logger logger, String entityName, Object entity) {
    logger.warn("{} is null -> {}", entityName, entity == null);
  }

}
