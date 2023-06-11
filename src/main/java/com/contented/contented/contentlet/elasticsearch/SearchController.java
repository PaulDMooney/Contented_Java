package com.contented.contented.contentlet.elasticsearch;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SearchController.SEARCH_PATH)
public class SearchController {
    public static final String SEARCH_PATH = "search";
}
