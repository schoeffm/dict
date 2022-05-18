package de.bender.dict.control;

import de.bender.dict.model.Translation;

public interface OutputFormatter {

    boolean canHandle(OutputFormat format);

    String format(Translation translation);

    enum OutputFormat {
        json, raw, alfred
    }
}
