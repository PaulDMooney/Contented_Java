package com.contented.contented.contentlet;

import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface ContentletRepository extends ListCrudRepository<ContentletEntity, UUID> {

}
