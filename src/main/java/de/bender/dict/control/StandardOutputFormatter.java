package de.bender.dict.control;

import de.bender.dict.model.Translation;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StandardOutputFormatter implements OutputFormatter {

    private static final int MAX_RESULTS = 20;
    private static final int HALF_LINE = 40;

    @Override
    public boolean canHandle(OutputFormat format) {
        return OutputFormat.raw.equals(format);
    }

    @Override
    public String format(Translation translation) {
        StringBuilder out = new StringBuilder();

        out.append("Term: ").append(translation.getQuery()).append("\n");
        out.append("=".repeat(70)).append("\n");

        if (translation.getGerman().isEmpty() || translation.getEnglish().isEmpty()) {
            out.append("Not Found");
        } else {
            var englishWords = translation.getEnglish();
            var germanWords = translation.getGerman();
            var max_iteration = Math.min(MAX_RESULTS,
                    Math.min(englishWords.size(), germanWords.size()));
            for (int i = 0; i < max_iteration; i++) {
                out.append(englishWords.get(i))
                        .append(" ".repeat(HALF_LINE - englishWords.get(i).length()))
                        .append(germanWords.get(i))
                        .append("\n");
            }
        }

        return out.toString();
    }
}
