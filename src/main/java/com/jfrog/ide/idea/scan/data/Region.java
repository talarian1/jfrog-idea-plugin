package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "endColumn",
    "endLine",
    "startColumn",
    "startLine"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class Region {

    @JsonProperty("endColumn")
    private Integer endColumn;
    @JsonProperty("endLine")
    private Integer endLine;
    @JsonProperty("startColumn")
    private Integer startColumn;
    @JsonProperty("startLine")
    private Integer startLine;

    @JsonProperty("endColumn")
    public Integer getEndColumn() {
        return endColumn;
    }

    @JsonProperty("endColumn")
    public void setEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
    }

    @JsonProperty("endLine")
    public Integer getEndLine() {
        return endLine;
    }

    @JsonProperty("endLine")
    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    @JsonProperty("startColumn")
    public Integer getStartColumn() {
        return startColumn;
    }

    @JsonProperty("startColumn")
    public void setStartColumn(Integer startColumn) {
        this.startColumn = startColumn;
    }

    @JsonProperty("startLine")
    public Integer getStartLine() {
        return startLine;
    }

    @JsonProperty("startLine")
    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Region.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("endColumn");
        sb.append('=');
        sb.append(((this.endColumn == null)?"<null>":this.endColumn));
        sb.append(',');
        sb.append("endLine");
        sb.append('=');
        sb.append(((this.endLine == null)?"<null>":this.endLine));
        sb.append(',');
        sb.append("startColumn");
        sb.append('=');
        sb.append(((this.startColumn == null)?"<null>":this.startColumn));
        sb.append(',');
        sb.append("startLine");
        sb.append('=');
        sb.append(((this.startLine == null)?"<null>":this.startLine));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.endLine == null)? 0 :this.endLine.hashCode()));
        result = ((result* 31)+((this.endColumn == null)? 0 :this.endColumn.hashCode()));
        result = ((result* 31)+((this.startColumn == null)? 0 :this.startColumn.hashCode()));
        result = ((result* 31)+((this.startLine == null)? 0 :this.startLine.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Region) == false) {
            return false;
        }
        Region rhs = ((Region) other);
        return (((((this.endLine == rhs.endLine)||((this.endLine!= null)&&this.endLine.equals(rhs.endLine)))&&((this.endColumn == rhs.endColumn)||((this.endColumn!= null)&&this.endColumn.equals(rhs.endColumn))))&&((this.startColumn == rhs.startColumn)||((this.startColumn!= null)&&this.startColumn.equals(rhs.startColumn))))&&((this.startLine == rhs.startLine)||((this.startLine!= null)&&this.startLine.equals(rhs.startLine))));
    }

}
