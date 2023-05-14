package com.contented.contented.contentlet;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ContentletController.CONTENTLETS_PATH)
public class ContentletController {

    public static final String CONTENTLETS_PATH = "contentlets";
}
