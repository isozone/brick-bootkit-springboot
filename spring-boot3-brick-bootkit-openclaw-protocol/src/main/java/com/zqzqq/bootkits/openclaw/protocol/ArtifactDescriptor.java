package com.zqzqq.bootkits.openclaw.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArtifactDescriptor {

    private String artifactId;
    private String name;
    private String contentType;
    private String uri;
    private Long sizeBytes;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public ArtifactDescriptor() {
    }

    public ArtifactDescriptor(ArtifactDescriptor source) {
        if (source == null) {
            return;
        }
        this.artifactId = source.artifactId;
        this.name = source.name;
        this.contentType = source.contentType;
        this.uri = source.uri;
        this.sizeBytes = source.sizeBytes;
        this.metadata = new LinkedHashMap<>(source.metadata);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }
}
