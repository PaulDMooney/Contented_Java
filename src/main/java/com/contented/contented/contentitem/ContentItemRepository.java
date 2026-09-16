package com.contented.contented.contentitem;

import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemState;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentItemRepository extends ListCrudRepository<ContentItemEntity, UUID> {

    // History for a logical content, newest version first.
    List<ContentItemEntity> findByIdentifierOrderByVersionCreatedDatetimeDesc(UUID identifier);

    // Safe as Optional only for the singleton states (LIVE, WORKING), which the partial unique
    // indexes guarantee are at most one per identifier.
    Optional<ContentItemEntity> findByIdentifierAndState(UUID identifier, ContentItemState state);

    boolean existsByIdentifier(UUID identifier);

    void deleteByIdentifier(UUID identifier);
}
