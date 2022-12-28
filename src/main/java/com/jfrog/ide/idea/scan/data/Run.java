package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"tool", "invocations", "results"})
public class Run {

    @JsonProperty("tool")
    private Tool tool;
    @JsonProperty("invocations")
    private List<Invocation> invocations = new ArrayList<>();
    @JsonProperty("results")
    private List<SarifResult> results = new ArrayList<>();

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public List<Invocation> getInvocations() {
        return invocations;
    }

    public void setInvocations(List<Invocation> invocations) {
        this.invocations = invocations;
    }

    public List<SarifResult> getResults() {
        return results;
    }

    public void setResults(List<SarifResult> results) {
        this.results = results;
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, tool, invocations);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Run)) {
            return false;
        }
        Run rhs = ((Run) other);
        return (((CollectionUtils.isEqualCollection(this.results, rhs.results)) && (Objects.equals(this.tool, rhs.tool))) && (CollectionUtils.isEqualCollection(this.invocations, rhs.invocations)));
    }

}
