package com.contented.contented.contentlet;

import java.util.Map;

public record CreateContentletCommand(String contentType, Map<String, Object> data) {
}
