package com.konkerlabs.platform.registry.business.repositories;

import com.konkerlabs.platform.registry.business.model.RestDestination;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RestDestinationRepository extends MongoRepository<RestDestination, String>{

    @Query("{ 'tenant.id' : ?0, 'guid' : ?1 }")
    RestDestination getByTenantAndGUID(String tenantId, String id);

    @Query("{ 'tenant.id' : ?0, 'name' : ?1 }")
    RestDestination getByTenantAndName(String tenantId, String name);
    
    @Query("{ 'tenant.id' : ?0 }")
    List<RestDestination> findAllByTenant(String tenantId);
}
