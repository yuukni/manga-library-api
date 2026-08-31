package com.example.mangalibraryapi.integration.mal;

import com.example.mangalibraryapi.integration.mal.dto.MalSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MyAnimeListClient {

    private final RestClient restClient;

    // Inject ID from application.yml and configure custom URI factory
    public MyAnimeListClient(@Value("${mal.client.id}") String clientId) {
        // Disable template expansion parsing to allow curly braces in parameters
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.restClient = RestClient.builder()
                .uriBuilderFactory(uriFactory) // Forces the client to ignore curly braces template parsing
                .baseUrl("https://api.myanimelist.net/v2")
                .defaultHeader("X-MAL-CLIENT-ID", clientId)
                .build();
    }

    /**
     * Searches for manga via the MyAnimeList API using specific field filters.
     * Requests chapters, volumes, authors, genres, status, and synopsis to fit local requirements.
     *
     * @param query The search term or title (e.g., "Naruto")
     * @param limit The maximum number of results to fetch (capped at 100)
     * @return A {@link MalSearchResponse} mapping the nested JSON response
     */
    public MalSearchResponse searchManga(String query, int limit) {
        String fields = "num_chapters,num_volumes,authors{first_name,last_name},media_type,status,mean,genres,synopsis";

        // Build and manually encode the URI beforehand
        java.net.URI targetUri = UriComponentsBuilder
                .fromUriString("https://api.myanimelist.net/v2/manga")
                .queryParam("q", query)
                .queryParam("limit", Math.min(limit, 100))
                .queryParam("fields", fields)
                .build()
                .toUri();

        return this.restClient.get()
                .uri(targetUri) // Will now pass safely since encodingMode is set to NONE
                .retrieve()
                .body(MalSearchResponse.class);
    }
}