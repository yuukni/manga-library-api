package com.example.mangalibraryapi.integration.mal;

import com.example.mangalibraryapi.integration.mal.dto.MalSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class MalSearchResponseJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDeserializeMalResponse() throws Exception {
        String json = """
            {
              "data": [
                {
                  "node": {
                    "id": 1,
                    "title": "Naruto",
                    "num_chapters": 700,
                    "authors": [
                      {"node":{"id":10,"first_name":"Masashi","last_name":"Kishimoto"},"role":"Story"}
                    ]
                  }
                }
              ]
            }
            """;

        MalSearchResponse response = objectMapper.readValue(json, MalSearchResponse.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).node().title()).isEqualTo("Naruto");
        assertThat(response.data().get(0).node().authors().get(0).node().first_name())
                .isEqualTo("Masashi");
    }
}