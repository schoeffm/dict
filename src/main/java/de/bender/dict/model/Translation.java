package de.bender.dict.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import static java.util.Collections.emptyList;

@RegisterForReflection
public class Translation {

    private final String query;
    private List<String> source;
    private List<String> destination;

    public Translation(String queryTerm) {
        this.query = queryTerm;
        this.source = emptyList();
        this.destination = emptyList();
    }

    public String getQuery() {
        return query;
    }

    public List<String> getSource() {
        return List.copyOf(source);
    }

    public List<String> getDestination() {
        return List.copyOf(destination);
    }

    public void setSource(List<String> source) {
        this.source = List.copyOf(source);
    }

    public void setDestination(List<String> destination) {
        this.destination = List.copyOf(destination);
    }

    @Override
    public String toString() {
        return "Translation{" +
                "query='" + query + '\'' +
                ", source=" + source +
                ", destination=" + destination +
                '}';
    }
}
