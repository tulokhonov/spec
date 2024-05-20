package com.example.spec.service;

import com.example.spec.service.entity.Person;
import com.example.spec.service.exception.FilterException;
import com.example.spec.service.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DynamicFilterService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final PersonRepository personRepository;

    public JsonNode convertJSONToNode(String json) throws IOException {
        return mapper.readTree(json);
    }

    public List<Person> getAllPersons(JsonNode jsonFilter) {
        Specification<Person> s = parseFilters(jsonFilter);
        return personRepository.findAll(s);
    }

    public <T> Specification<T> parseFilters(JsonNode node) {

        if (node.isObject()) {

            Iterator<Map.Entry<String, JsonNode>> fieldIterator = node.fields();

            if (!fieldIterator.hasNext()) {
                throw new FilterException("Empty object! Cannot determine predicate or logic operation");
            }
            Map.Entry<String, JsonNode> filedEntry = fieldIterator.next();

            if (filedEntry.getKey().equals("AND") || (filedEntry.getKey().equals("OR"))) {

                log.debug(filedEntry.getKey().equals("AND") ? "AND" : "OR");

                if (!filedEntry.getValue().isArray()) {
                    throw new FilterException("Array is expected for logic operations: " + node);
                }

                Iterator<JsonNode> arrayItemsIterator = filedEntry.getValue().elements();
                if (!arrayItemsIterator.hasNext()) throw new FilterException("Empty predicates list: " + node);

                List<Specification<T>> specifications = new ArrayList<>();
                while (arrayItemsIterator.hasNext()) {
                    JsonNode n = arrayItemsIterator.next();
                    Specification<T> specification = parseFilters(n);
                    specifications.add(specification);
                }

                BinaryOperator<Specification<T>> operator = filedEntry.getKey().equals("AND")
                        ? Specification::and
                        : Specification::or;

                Specification<T> combinedSpec = specifications.stream()
                        .reduce(operator)
                        .orElseThrow(() -> new FilterException("Error combining specifications"));

                return combinedSpec;
            } else {
                return parseJsonNode(node);
            }
        } else
            throw new FilterException("Object is expected to describe predicate or logic operations!");
    }

    private <T> Specification<T> parseJsonNode(JsonNode node) {

        try {
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = node.fields();

            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();

            if (fieldIterator.hasNext()) throw new IllegalArgumentException("Only one predicate is allowed: " + node);

            String field = fieldEntry.getKey();

            if (!fieldEntry.getValue().isObject()) {
                throw new IllegalArgumentException("Object is expected to describe predicate for field: " + field);
            }
            if (!fieldEntry.getValue().fields().hasNext()) {
                throw new IllegalArgumentException("No value(s) for predicate");
            }
            Map.Entry<String, JsonNode> opValueEntry = fieldEntry.getValue().fields().next();
            
            String operation = opValueEntry.getKey();
            JsonNode valueNode = opValueEntry.getValue();
            
            return getSpecification(field, valueNode, operation);

        } catch (Exception e) {
            log.error("Error!", e);
            throw new FilterException("Error parsing predicate!", e);
        }
    }

    private <T> Specification<T> getSpecification(String field, JsonNode valueNode, String operation) {
        
        String value = valueNode.asText();
        
        log.debug("{} {} {}", field, operation, value);
        
        return (root, cq, cb) -> {
            Path<Object> path = root.get(field);

            switch (operation) {
                case "eq": {
                    if (!valueNode.isNull()) {
                        return cb.equal(path, parseStringToType(path.getJavaType(), value));
                    } else {
                        return cb.isNull(path);
                    }
                }
                case "!eq": {
                    if (!valueNode.isNull()) {
                        return cb.notEqual(path, parseStringToType(path.getJavaType(), value));
                    } else {
                        return cb.isNotNull(path);
                    }
                }
                case "gt": {
                    if (isLocalDateTime(path)) {
                        return cb.greaterThan(root.get(field), LocalDateTime.parse(value));
                    } else if (isLocalDate(path)) {
                        return cb.greaterThan(root.get(field), LocalDate.parse(value));
                    }else if (isNumber(path)) {
                        return cb.gt(root.get(field), (Number) parseStringToType(path.getJavaType(), value));
                    }
                    throw new IllegalArgumentException("'%s' operation supported only for numbers and dates".formatted(operation));
                }
                case "ge": {
                    if (isLocalDateTime(path)) {
                        return cb.greaterThanOrEqualTo(root.get(field), LocalDateTime.parse(value));
                    } else if (isLocalDate(path)) {
                        return cb.greaterThanOrEqualTo(root.get(field), LocalDate.parse(value));
                    } else if (isNumber(path)) {
                        return cb.ge(root.get(field), (Number) parseStringToType(path.getJavaType(), value));
                    }
                    throw new IllegalArgumentException("'%s' operation supported only for numbers and dates".formatted(operation));
                }
                case "lt": {
                    if (isLocalDateTime(path)) {
                        return cb.lessThan(root.get(field), LocalDateTime.parse(value));
                    } else if (isLocalDate(path)) {
                        return cb.lessThan(root.get(field), LocalDate.parse(value));
                    } else if (isNumber(path)) {
                        return cb.lt(root.get(field), (Number) parseStringToType(path.getJavaType(), value));
                    }
                    throw new IllegalArgumentException("'%s' operation supported only for numbers and dates".formatted(operation));
                }
                case "le": {
                    if (isLocalDateTime(path)) {
                        return cb.lessThanOrEqualTo(root.get(field), LocalDateTime.parse(value));
                    } else if (isLocalDate(path)) {
                        return cb.lessThanOrEqualTo(root.get(field), LocalDate.parse(value));
                    } else if (isNumber(path)) {
                        return cb.le(root.get(field), (Number) parseStringToType(path.getJavaType(), value));
                    }
                    throw new IllegalArgumentException("'%s' operation supported only for numbers and dates".formatted(operation));
                }
                case "in": {
                    if (!valueNode.isArray()) {
                        throw new IllegalArgumentException("'In' operation supported only for arrays");
                    }
                    List<String> values = getValues(valueNode);
                    return cb.in(root.get(field))
                            .value(parseStringToType(path.getJavaType(), values));
                }
                case "!in": {
                    if (!valueNode.isArray()) {
                        throw new IllegalArgumentException("'!In' operation supported only for arrays");
                    }
                    List<String> values = getValues(valueNode);
                    return cb.in(root.get(field))
                            .value(parseStringToType(path.getJavaType(), values))
                            .not();
                }
                default:
                    throw new IllegalArgumentException("'%s' operation is not supported!".formatted(operation));
            }
        };
    }

    private List<String> getValues(JsonNode valueNode) {
        List<String> list = new ArrayList<>();
        if (valueNode.isArray()) {
            Iterator<JsonNode> iterator = valueNode.elements();
            while (iterator.hasNext()) {
                list.add(iterator.next().asText());
            }
        }
        return list;
    }

    private static boolean isNumber(Path<Object> path) {
        return Number.class.isAssignableFrom(path.getJavaType());
    }

    private static boolean isLocalDateTime(Path<Object> path) {
        return path.getJavaType().isAssignableFrom(LocalDateTime.class);
    }

    private static boolean isLocalDate(Path<Object> path) {
        return path.getJavaType().isAssignableFrom(LocalDate.class);
    }

    private Object parseStringToType(Class<?> fieldType, String value) {
        if (Number.class.isAssignableFrom(fieldType)) {
            if (fieldType.isAssignableFrom(BigDecimal.class)) {
                return new BigDecimal(value);
            } else if (fieldType.isAssignableFrom(Integer.class)) {
                return Integer.valueOf(value);
            } else if (fieldType.isAssignableFrom(Long.class)) {
                return Long.valueOf(value);
            } else if (fieldType.isAssignableFrom(Double.class)) {
                return Double.valueOf(value);
            } else if (fieldType.isAssignableFrom(Float.class)) {
                return Float.valueOf(value);
            } else if (fieldType.isAssignableFrom(Short.class)) {
                return Short.valueOf(value);
            } else if (fieldType.isAssignableFrom(Byte.class)) {
                return Byte.valueOf(value);
            }
        } else if (fieldType.isAssignableFrom(String.class)) {
            return value;
        } else if (fieldType.isAssignableFrom(LocalDateTime.class)) {
            return LocalDateTime.parse(value);
        } else if (fieldType.isAssignableFrom(LocalDate.class)) {
            return LocalDate.parse(value);
        }else if (fieldType.isAssignableFrom(Boolean.class)) {
            return Boolean.valueOf(value);
        }
        throw new FilterException("Field type '%s' is not supported!".formatted(fieldType));
    }

    private Object parseStringToType(Class<?> fieldType, List<String> values) {
        return values.stream()
                .map(e -> parseStringToType(fieldType, e))
                .collect(Collectors.toList());
    }
}