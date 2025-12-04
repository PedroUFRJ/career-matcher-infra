package br.com.careermatcher.lambda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetAllVagasHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final Driver driver;
    private static final ObjectMapper JSON = new ObjectMapper();

    public GetAllVagasHandler() {
        driver = GraphDatabase.driver(
                System.getenv("NEO4J_URI"),
                AuthTokens.basic(System.getenv("NEO4J_USERNAME"), System.getenv("NEO4J_PASSWORD"))
        );
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        // --- 1) Reconstituir 'filters' aceitando ambos os formatos ---
        Map<String, Object> filters = new LinkedHashMap<>();

        if (input != null) {
            // caso 1: filters no nível raiz (invocação direta)
            Object rootFilters = input.get("filters");
            if (rootFilters instanceof Map<?, ?>) {
                filters.putAll((Map<String, Object>) rootFilters);
            } else {
                // caso 2: body string (API Gateway proxy). Tentar desserializar.
                Object bodyObj = input.get("body");
                if (bodyObj instanceof String) {
                    String bodyStr = (String) bodyObj;
                    if (!bodyStr.isBlank()) {
                        try {
                            Map<String, Object> parsed = JSON.readValue(bodyStr, new TypeReference<>() {});
                            Object parsedFilters = parsed.get("filters");
                            if (parsedFilters instanceof Map<?, ?>) {
                                filters.putAll((Map<String, Object>) parsedFilters);
                            }
                        } catch (Exception e) {
                            // não falha aqui; se não for JSON válido, continua com filters vazio
                            context.getLogger().log("Warning: não foi possível parsear body JSON: " + e.getMessage());
                        }
                    }
                }
            }
        }

        // --- 2) Normalizar filtros ---
        List<String> senioridade = normalizeList(filters.get("senioridade"));
        List<String> modalidade  = normalizeList(filters.get("modalidade"));
        List<String> cargo       = normalizeList(filters.get("cargo"));
        List<String> empresa     = normalizeList(filters.get("empresa"));
        List<String> cidade      = normalizeList(filters.get("cidade"));

        List<Map<String, Object>> vagas = new ArrayList<>();

        try (Session session = driver.session()) {

            String query =
            """
            MATCH (v:VAGA)
            WHERE 
                ($senioridade IS NULL OR ANY(s IN $senioridade WHERE toLower(v.senioridade) CONTAINS toLower(s))) AND
                ($modalidade  IS NULL OR ANY(m IN $modalidade  WHERE toLower(v.modalidade)  CONTAINS toLower(m))) AND
                ($cargo       IS NULL OR ANY(c IN $cargo       WHERE toLower(v.cargo)       CONTAINS toLower(c))) AND
                ($empresa     IS NULL OR ANY(e IN $empresa     WHERE toLower(v.empresa)     CONTAINS toLower(e))) AND
                ($cidade      IS NULL OR ANY(ci IN $cidade     WHERE toLower(v.cidade)      CONTAINS toLower(ci)))

            OPTIONAL MATCH (v)-[r]->(n)
            OPTIONAL MATCH (cand:CANDIDATO {id: v.idCandidatoEscolhido})
            OPTIONAL MATCH (cand)-[cr]->(cn)

            RETURN v,
                cand AS candidato,
                collect(DISTINCT {
                    type: type(r),
                    target: n,
                    peso: r.peso
                }) AS relationshipsVaga,
                collect(DISTINCT {
                    type: type(cr),
                    target: cn,
                    peso: cr.peso
                }) AS relationshipsCandidato
            """;

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("senioridade", senioridade);
            params.put("modalidade", modalidade);
            params.put("cargo", cargo);
            params.put("empresa", empresa);
            params.put("cidade", cidade);

            Result result = session.run(query, params);

            while (result.hasNext()) {
                Record rec = result.next();

                var vagaNode = rec.get("v").asNode();
                Value candVal = rec.get("candidato");

                Map<String, Object> candidatoMap = candVal != null && !candVal.isNull()
                        ? candVal.asNode().asMap() : null;

                Map<String, Object> vaga = new LinkedHashMap<>();
                vaga.put("id", vagaNode.id());

                Map<String, Object> props = new LinkedHashMap<>(vagaNode.asMap());
                props.put("candidatoEscolhido", candidatoMap);
                props.put("relationshipsVaga", rel(rec.get("relationshipsVaga")));
                props.put("relationshipsCandidato", rel(rec.get("relationshipsCandidato")));

                vaga.put("properties", props);
                vagas.add(vaga);
            }
        } catch (Exception e) {
            // log e retorna 500 com erro simples
            Map<String, Object> err = Map.of("error", e.getMessage());
            return buildResponse(500, err);
        }

        // monta body JSON com ObjectMapper (string limpa, sem escapes extras)
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("vagas", vagas);

        return buildResponse(200, bodyMap);
    }

    // monta resposta padrão com body (string JSON) e CORS
    private Map<String, Object> buildResponse(int status, Object bodyObj) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statusCode", status);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Access-Control-Allow-Origin", "https://julopesrocha.github.io");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Content-Type", "application/json; charset=utf-8");
        response.put("headers", headers);

        try {
            String bodyJson = JSON.writeValueAsString(bodyObj);
            response.put("body", bodyJson);
        } catch (Exception e) {
            response.put("body", "{\"error\":\"failed to serialize body\"}");
        }
        return response;
    }

    private List<String> normalizeList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (o != null && !o.toString().trim().isEmpty()) result.add(o.toString().trim());
            }
            return result.isEmpty() ? null : result;
        }
        if (value instanceof String s) return s.trim().isEmpty() ? null : List.of(s.trim());
        return null;
    }

    private List<Map<String, Object>> rel(Value value) {
        if (value == null || value.isNull()) return new ArrayList<>();
        return value.asList(item -> {
            Value v = (Value) item;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", v.get("type").isNull() ? null : v.get("type").asString());
            r.put("target", v.get("target").isNull() ? null : v.get("target").asNode().asMap());
            r.put("peso", v.get("peso").isNull() ? null : v.get("peso").asLong());
            return r;
        });
    }
}
