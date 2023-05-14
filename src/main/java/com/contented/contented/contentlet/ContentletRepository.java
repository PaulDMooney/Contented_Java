package com.contented.contented.contentlet;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ContentletRepository extends ReactiveMongoRepository<ContentletEntity, String> {

}
