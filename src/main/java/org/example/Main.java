package org.example;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DocumentManager manager = new DocumentManager();

        DocumentManager.Author author = new DocumentManager.Author("1", "Author");
        DocumentManager.Document doc = new DocumentManager.Document("1", "PrefixHello",
                "Test data",
                author,
                Instant.now());

        DocumentManager.Author author2 = new DocumentManager.Author("2", "Author 2");
        DocumentManager.Document doc2 = new DocumentManager.Document("2",
                "AgainHelloAgain",
                "Test data second",
                author2,
                Instant.now());

        DocumentManager.Author author3 = new DocumentManager.Author("3", "Author 3");
        DocumentManager.Document doc3 = new DocumentManager.Document("3",
                "HelloTripleAgain",
                "Test data third",
                author3,
                Instant.now());

        DocumentManager.Author author4 = new DocumentManager.Author("4", "Author 4");
        DocumentManager.Document doc4 = new DocumentManager.Document("4",
                "HelloTriplePlusOneAgain",
                "Test data forth",
                author4,
                Instant.now());

        manager.save(doc);
        manager.save(doc2);
        manager.save(doc3);
        manager.save(doc4);

        DocumentManager.SearchRequest request = DocumentManager.SearchRequest.builder()
                .titlePrefixes(new ArrayList<>())
                .authorIds(new ArrayList<>())
                .containsContents(new ArrayList<>(Arrays.asList("data", "forth")))
                .createdFrom(null)
                .createdTo(null)
                .build();

        List<DocumentManager.Document> list =  manager.search(request);

        System.out.println(manager.findById("1"));

        list.stream().forEach( i -> System.out.println("Result  --> " + i ));


    }
}