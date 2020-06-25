package me.madfix.projects.java.cloudnet.webinterface.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.dytanic.cloudnet.lib.server.priority.PriorityConfig;
import de.dytanic.cloudnet.lib.server.priority.PriorityService;
import java.lang.reflect.Type;

public class PriorityServiceJsonAdapter implements JsonSerializer<PriorityService>,
    JsonDeserializer<PriorityService> {

  @Override
  public PriorityService deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    JsonObject object = jsonElement.getAsJsonObject();
    final int stopTimeInSeconds = object.get("stopTimeInSeconds").getAsInt();
    final PriorityConfig global = jsonDeserializationContext
        .deserialize(object.get("global"), PriorityConfig.class);
    final PriorityConfig group = jsonDeserializationContext
        .deserialize(object.get("group"), PriorityConfig.class);
    return new PriorityService(stopTimeInSeconds, global, group);
  }

  @Override
  public JsonElement serialize(PriorityService priorityService, Type type,
      JsonSerializationContext jsonSerializationContext) {
    JsonObject object = new JsonObject();
    object.addProperty("stopTimeInSeconds", priorityService.getStopTimeInSeconds());
    object.add("global", jsonSerializationContext.serialize(priorityService.getGlobal()));
    object.add("group", jsonSerializationContext.serialize(priorityService.getGroup()));
    return object;
  }
}