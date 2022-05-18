package de.bender.dict.control;

import de.bender.dict.model.Translation;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class JsonOutputFormatter implements OutputFormatter {

    private final Jsonb jsonb;

    public JsonOutputFormatter() {
        this.jsonb = JsonbBuilder.create();
    }

    @Override
    public boolean canHandle(OutputFormat format) {
        return OutputFormat.json.equals(format);
    }

    @Override
    public String format(Translation translation) {
        Map<String, Object> jsonStruct = new HashMap<>();

        jsonStruct.put("query", translation.getQuery());
        jsonStruct.put("english", translation.getEnglish());
        jsonStruct.put("german", translation.getGerman());

        return jsonb.toJson(jsonStruct);
    }
}
