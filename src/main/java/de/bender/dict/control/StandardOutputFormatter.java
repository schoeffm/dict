package de.bender.dict.control;

import de.bender.dict.model.Translation;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StandardOutputFormatter implements OutputFormatter {

    private static final int MAX_RESULTS = 20;
    private static final int HALF_LINE = 50;
    private static final int FULL_LINE = 100;

    @Override
    public boolean canHandle(OutputFormat format) {
        return OutputFormat.raw.equals(format);
    }

    @Override
    public String format(Translation translation) {
        StringBuilder out = new StringBuilder();

        out.append("Term: ").append(translation.getQuery()).append("\n");
        out.append("=".repeat(FULL_LINE)).append("\n");

        if (translation.getSource().isEmpty() || translation.getDestination().isEmpty()) {
            out.append("Not Found");
        } else {
            var englishWords = translation.getDestination();
            var germanWords = translation.getSource();
            var max_iteration = Math.min(MAX_RESULTS,
                    Math.min(englishWords.size(), germanWords.size()));

            for (int i = 0; i < max_iteration; i++) {
                out.append(englishWords.get(i))
                        .append(" ".repeat(HALF_LINE - englishWords.get(i).length() > 0 ? HALF_LINE - englishWords.get(i).length() : 1))
                        .append(germanWords.get(i))
                        .append("\n");
            }
        }

        return out.toString();
    }
}
