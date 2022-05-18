package de.bender.dict.control;

import de.bender.dict.model.Translation;

import javax.enterprise.context.ApplicationScoped;

import static de.bender.dict.control.OutputFormatter.OutputFormat.alfred;

@ApplicationScoped
public class AlfredOutputFormatter implements OutputFormatter {

    private static final int MAX_RESULTS = 20;

    @Override
    public boolean canHandle(OutputFormat format) {
        return alfred.equals(format);
    }

    @Override
    public String format(Translation translation) {
        StringBuilder out = new StringBuilder();
        out.append("<?xml version=\"1.0\"?>");
        out.append("<items>");
        if (translation.getGerman().isEmpty() || translation.getEnglish().isEmpty()) {
            out.append("<item valid=\"no\">");
            out.append("<title>").append(translation.getQuery()).append(" not found</title>");
            out.append("<icon>de_en.png</icon>");   // TODO: configurierbar
            out.append("</item>");
        } else {
            var englishWords = translation.getEnglish();
            var germanWords = translation.getGerman();
            var max_iteration = Math.min(MAX_RESULTS,
                    Math.min(englishWords.size(), germanWords.size()));
            for (int i = 0; i < max_iteration; i++) {
                out.append("<item valid=\"yes\" arg=\"").append(englishWords.get(i)).append("\">");
                out.append("<title>").append(englishWords.get(i)).append("</title>");
                out.append("<subtitle>").append(germanWords.get(i)).append("</subtitle>");
                out.append("<icon>de_en.png</icon>");   // TODO: configurierbar
                out.append("</item>");
            }
        }
        out.append("</items>");
        return out.toString();
    }
}
