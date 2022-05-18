package de.bender.dict.model;

import java.util.List;

import static java.util.Collections.emptyList;

public class Translation {

    private final String query;
    private List<String> german;
    private List<String> english;

    public Translation(String queryTerm) {
        this.query = queryTerm;
        this.german = emptyList();
        this.english = emptyList();
    }

    public String getQuery() {
        return query;
    }

    public List<String> getGerman() {
        return List.copyOf(german);
    }

    public List<String> getEnglish() {
        return List.copyOf(english);
    }

    public void setGerman(List<String> german) {
        this.german = List.copyOf(german);
    }

    public void setEnglish(List<String> english) {
        this.english = List.copyOf(english);
    }

    @Override
    public String toString() {
        return "Translation{" +
                "query='" + query + '\'' +
                ", german=" + german +
                ", english=" + english +
                '}';
    }
}
