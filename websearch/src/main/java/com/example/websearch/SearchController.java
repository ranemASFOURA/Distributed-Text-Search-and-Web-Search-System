package com.example.websearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam("query") String query, Model model) {

        SearchResponse searchResponse = searchService.sendQueryToLeader(query);


        model.addAttribute("query", query);
        model.addAttribute("results", WebSearchResult.getResults());


        return "index";
    }

}
