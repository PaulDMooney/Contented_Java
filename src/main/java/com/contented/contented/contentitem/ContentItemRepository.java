package com.contented.contented.contentitem;

import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface ContentItemRepository extends ListCrudRepository<ContentItemEntity, UUID> {

}
