package com.konkerlabs.platform.registry.business.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Document(collection = "dataEnrichmentExtensions")
@Data
@Builder
public class DataEnrichmentExtension {

    public static enum EnrichmentType {REST};
    
    @Id
    private String id;
    @DBRef
    private Tenant tenant;
    private String name;
    private EnrichmentType type;
    private String description;

    private URI incoming;
    private String containerKey;

    @Singular
    private Map<String, String> parameters = new HashMap<>();

    private boolean active;

    public List<String> applyValidations() {
        List<String> validations = new ArrayList<>();

        if (getTenant() == null)
            validations.add("Tenant cannot be null");
        
        if (getName() == null || getName().isEmpty())
            validations.add("Name cannot be null or empty");
        
        if (getIncoming() == null)
            validations.add("Incoming device cannot be null");

        if (!"device".equals(Optional.ofNullable(getIncoming()).orElse(URI.create("")).getScheme()))
            validations.add("Incoming must be a device");

        if (getType() == null)
            validations.add("Data enrichment type cannot be null");

        if (parameters == null)
            validations.add("Parameters cannot be null");

            
        if (validations.isEmpty())
            return null;
        else
            return validations;
    }
}
