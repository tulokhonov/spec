package com.example.spec.service;

import com.example.spec.service.entity.Person;
import com.example.spec.service.exception.WalkerException;
import com.example.spec.service.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

@RequiredArgsConstructor
@Service
public class TreeWalker {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(TreeWalker.class);
    private final PersonRepository personRepository;

    public JsonNode convertJSONToNode(String json) throws IOException {
        return mapper.readTree(json);
    }

    public List<Person> walkTree(JsonNode root) {
        Specification<Person> s = walker(root);
        return personRepository.findAll(s);
    }

    public <T> Specification<T> walker(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            Map.Entry<String, JsonNode> e = iterator.next();
            if (e.getKey().equals("AND") || (e.getKey().equals("OR"))) {

                log.info(e.getKey().equals("AND")? "AND" : "OR");

                Iterator<JsonNode> arrayItemsIterator = e.getValue().elements();

                List<Specification<T>> specifications = new ArrayList<>();
                while (arrayItemsIterator.hasNext()) {
                    JsonNode n = arrayItemsIterator.next();
                    Specification<T> s = walker(n);
                    specifications.add(s);
                }

                BinaryOperator<Specification<T>> operator = e.getKey().equals("AND")
                        ? Specification::and
                        : Specification::or;

                Specification<T> combinedSpec = specifications.stream()
                        .reduce(operator)
                        .orElseThrow(RuntimeException::new);

                return combinedSpec;
            } else {
                return getSpec(node);
            }
        } else
            throw new WalkerException("Invalid JSON structure");
    }

    private <T> Specification<T> getSpec(JsonNode node) {

        try {
            if (!node.isObject())
                throw new IllegalArgumentException("Object is expected");
            if (!node.fields().hasNext())
                throw new IllegalArgumentException("Invalid JSON structure");

            Map.Entry<String, JsonNode> fieldEntry = node.fields().next();
            String field = fieldEntry.getKey();

            if (!fieldEntry.getValue().isObject())
                throw new IllegalArgumentException("Object is expected for field: " + field);

            if (!fieldEntry.getValue().fields().hasNext())
                throw new IllegalArgumentException("Invalid JSON structure");

            Map.Entry<String, JsonNode> opValueEntry = fieldEntry.getValue().fields().next();
            String op = opValueEntry.getKey();
            JsonNode vNode = opValueEntry.getValue();
            Object value = vNode.isTextual()
                    ? vNode.asText()
                    : vNode.isNumber()
                    ? vNode.asInt()
                    : vNode.textValue();

            log.info("{} {} {}", field, op, value);
            return (entity, cq, cb) -> cb.equal(entity.get(field), value);

        } catch (Exception e) {
            // Implement exception translation
            log.error("Error!", e);
            throw new WalkerException("Invalid JSON structure!", e);
        }
    }
}