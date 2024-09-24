package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.*;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {

    private static final String STORAGE_FILES_PATH = "src/main/resources/storage";
    private static final String STORAGE_DATA_PATH = "src/main/resources/documentsData.txt";
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        //enabling serializing  instant object
        objectMapper.registerModule(new JavaTimeModule());
    }



    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */

    public Document save(Document document) {
        if(checkAndCreateFolder(STORAGE_FILES_PATH)){

            Document docCreated = saveDocumentData(document);

            File file = new File(STORAGE_FILES_PATH + "/"  + document.getTitle() + "_" + document.getId());

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))){

                //save and put creation time
                writer.write(document.getContent());
                document.setCreated(Instant.now());

                return docCreated;


            }catch (IOException err){

                System.out.println(err.getMessage());

                return null;
            }

        }else{
            throw new RuntimeException("There isnt folder " + STORAGE_FILES_PATH);
        }


    }


    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {

        List<Document> filteredDocuments = getDocumentsList();


        //DATE FILTER
        filteredDocuments = filterByDateCreation(filteredDocuments, request.getCreatedFrom(), request.getCreatedTo());

        //TITLE FILTER
        filteredDocuments = filterByPrefixes(filteredDocuments, request.getTitlePrefixes());

        //AUTHOR FILTER
        filteredDocuments = filterByAuthorIds(filteredDocuments, request.getAuthorIds());

        //CONTENT FILTER
        filteredDocuments = filterByContent(filteredDocuments, request.getContainsContents());

        return filteredDocuments;
    }

    private List<Document> filterByAuthorIds(List<Document> documents,
                                                List<String> authorsIds){

        if (authorsIds != null && !authorsIds.isEmpty()) {
            return documents.stream()
                    .filter(doc -> authorsIds.contains(doc.getAuthor().getId()))
                    .toList();
        }


        return documents;

    }

    private List<Document> filterByDateCreation(List<Document> documents,
                                                Instant createdFrom,
                                                Instant createdTo){

        //filter by timeCreation(created before request.createdTo)
        if (createdTo != null) {
            documents = documents.stream()
                    .filter(doc -> doc.getCreated().isBefore(createdTo))
                    .toList();
        }

        //filter by timeCreation(created after request.createdFrom)
        if (createdFrom != null) {
            documents = documents.stream()
                    .filter(doc -> doc.getCreated().isAfter(createdFrom))
                    .toList();
        }

        return documents;

    }

    private List<Document> filterByPrefixes(List<Document> documents,
                                            List<String> prefixes){
        if (prefixes != null && !prefixes.isEmpty()) {
            return documents.stream()
                    .filter(doc -> prefixes.stream()
                            .anyMatch(prefix -> doc.getTitle().startsWith(prefix)))
                    .toList();
        }
        return documents;

    }

    private List<Document> filterByContent(List<Document> documents,
                                           List<String> contentFilterWords) {
        List<Document> docWithContent = new ArrayList<>();

        //reading all content and adding to Document objects as field "content"
        for (Document doc : documents) {
            File file = new File(STORAGE_FILES_PATH + "/" + doc.getTitle() + "_" + doc.getId());

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder contentBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        contentBuilder.append(line).append(System.lineSeparator());
                    }

                    doc.setContent(contentBuilder.toString().trim());
                    docWithContent.add(doc);
                } catch (IOException err) {
                    System.out.println(err.getMessage());
                }
            }
        }

        //filtering by content
        return docWithContent.stream()
                .filter(doc -> checkAllContentPresent(doc.getContent(), contentFilterWords))
                .toList();


    }


    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {
        List<Document> documents = getDocumentsList();

        Document document =  documents.stream()
                .filter(doc -> doc.getId().equals(id))
                .findAny().orElse(null);

        if(document == null){
            return Optional.empty();
        }

        File file = new File(STORAGE_FILES_PATH + "/" + document.getTitle() + "_" + document.getId());

        if(file.exists()){
            try(BufferedReader reader = new BufferedReader(new FileReader(file))){
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append(System.lineSeparator());
                }
                document.setContent(contentBuilder.toString().trim());


            }catch (IOException err){
                System.out.println(err.getMessage());
            }
        }
        return Optional.of(document);

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Document {
        private String id;
        private String title;

        @JsonIgnore
        private String content;

        private Author author;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
        private Instant created;


    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Author {
        private String id;
        private String name;

    }


    private List<Document> getDocumentsList(){
        List<Document> documents = new ArrayList<>();
        File file = new File(STORAGE_DATA_PATH);


        //reading data from database
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                documents = objectMapper.readValue(reader, new TypeReference<ArrayList<Document>>(){});

            } catch (IOException err) {
                System.out.println(err.getMessage());

            }
        }

        //if file is empty
        if(documents == null){
            return new ArrayList<>();
        }

        return documents;


    }

    private Document saveDocumentData(Document document) {

        File file = new File(STORAGE_DATA_PATH);
        List<Document> documents = getDocumentsList();


        //if there is no default ID
        if(document.getId() == null){
            document.setId(UUID.randomUUID().toString());
        }

        //if there is already a doc with such an id
        if(document.getId() != null && findById(document.getId()).isPresent()){
            document.setId(UUID.randomUUID().toString());

        }

        documents.add(document);

        //re-saving data
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String jsonString = objectMapper.writeValueAsString(documents);
            writer.write(jsonString);

        } catch (IOException err) {
            System.out.println(err.getMessage());

        }

        return document;
    }

    public static boolean checkAllContentPresent(String text, List<String> filters) {
        for (String filter : filters) {
            if (!text.contains(filter)) {
                return false;
            }
        }
        return true;
    }


    public boolean checkAndCreateFolder(String folderPath) {
        Path path = Paths.get(folderPath);

        //check if folder exists
        if (Files.exists(path)) {
            return true; // Folder exists
        } else {
            try {
                //create folder
                Files.createDirectories(path);
                return true;
            } catch (Exception err) {
                System.out.println(err.getMessage());
                return false;
            }
        }
    }



}