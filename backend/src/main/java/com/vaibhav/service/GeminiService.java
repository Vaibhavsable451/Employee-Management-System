package com.vaibhav.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {
    @Value("${GEMINI_API_KEY}")
    private String apiKey;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String API_KEY = System.getenv("GEMINI_API_KEY");

    public Object askGeminiAndExecute(String prompt) {
        try {
            String aiResponse = askGemini(prompt);

            // ✅ Extract SQL query inside ```sql ... ```
            String sql = aiResponse.replaceAll("(?s).*```sql\\s*", "")
                    .replaceAll("```.*", "")
                    .trim();

            if (!sql.toLowerCase().startsWith("select")) {
                return "⚠️ Gemini did not return a SELECT query. Response: " + aiResponse;
            }

            // ✅ Execute query safely
            return jdbcTemplate.queryForList(sql);

        } catch (Exception e) {
            return "❌ Error executing AI-generated query: " + e.getMessage();
        }
    }

    public String askGemini(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            String systemPrompt = """
You are an expert SQL assistant.
Convert the user's request into a valid MySQL SELECT query for the 'employees' table only.

⚠️ Do not create fake or sample data.
⚠️ Do not describe what the query does.
✅ Only return the SQL query wrapped in ```sql ... ```.

Example outputs:
```sql
SELECT * FROM employees;
SELECT * FROM employees WHERE first_name LIKE 'V%';
SELECT * FROM employees WHERE id = 16;
SELECT * FROM employees WHERE first_name = 'Vaibhav';
SELECT DISTINCT email_id FROM employees;
SELECT COUNT(*) FROM employees WHERE first_name = 'John';
SELECT * FROM employees WHERE email_id LIKE '%gmail.com%';
SELECT id, first_name, last_name FROM employees;
SELECT * FROM employees ORDER BY first_name ASC;
SELECT * FROM employees WHERE id > 10;
SELECT email_id, COUNT(*) FROM employees GROUP BY email_id HAVING COUNT(*) > 1;
SELECT COUNT(*) FROM employees;
SELECT * FROM employees WHERE last_name != 'Sable';
SELECT * FROM employees WHERE id = (SELECT MAX(id) FROM employees);
"""; String requestBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" +
                    systemPrompt + "\\nQuestion: " + prompt + "\" }]}]}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String url = API_URL + "?key=" + apiKey;

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (Exception e) {
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}