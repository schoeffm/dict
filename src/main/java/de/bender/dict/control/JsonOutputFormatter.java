package de.bender.dict.control;

import de.bender.dict.model.Translation;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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
        return jsonb.toJson(translation);
    }
}
