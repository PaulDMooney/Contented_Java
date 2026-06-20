package com.contented.contented.contentlet;

import java.util.Map;
import java.util.UUID;

public record UpdateContentletCommand(UUID id, String contentType, Map<String, Object> data) {
}
