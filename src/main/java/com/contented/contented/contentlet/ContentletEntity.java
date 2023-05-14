package com.contented.contented.contentlet;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class ContentletEntity {

    @Id
    private final String id;

}
