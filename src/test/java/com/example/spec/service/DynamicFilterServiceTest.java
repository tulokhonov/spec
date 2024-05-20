package com.example.spec.service;

import com.example.spec.service.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@Sql("/data.sql")
@Transactional
class DynamicFilterServiceTest {

    @Autowired
    private DynamicFilterService treeWalker;

    @Test
    void test1() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                     "AND": [
                         {
                             "age": {
                                 "eq": 10
                             }
                         },
                         {
                             "OR": [
                                 {
                                     "name": {
                                         "eq": "Alice"
                                     }
                                 },
                                 {
                                     "name": {
                                         "eq": "Bob"
                                     }
                                 },
                                 {
                                     "name": {
                                         "eq": "Oleg"
                                     }
                                 },
                                 {
                                     "name": {
                                         "eq": "Olga"
                                     }
                                 }
                             ]
                         }
                     ]
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(2, list.size());
    }

    @Test
    void test2() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "age": {
                                    "gt": 10
                                }
                            }                        ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(2, list.size());
    }

    @Test
    void test3() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "score": {
                                    "ge": 33.2
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(4, list.size());
    }

    @Test
    void test4() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "date": {
                                    "ge": "2024-05-01T00:00:00"
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(6, list.size());
    }

    @Test
    void test5() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "date": {
                                    "lt": "2024-05-11T00:00:00"
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(4, list.size());
    }

    @Test
    void test6() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "date": {
                                    "le": "2024-05-11T12:00:00"
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(5, list.size());
    }

    @Test
    void test7() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "score": {
                                    "eq": null
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(1, list.size());
        assertEquals("Bob", list.get(0).getName());
    }

    @Test
    void test8() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "score": {
                                    "!eq": null
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(7, list.size());
    }

    @Test
    void test9() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "age": {
                                    "in": [10, 11, 12]
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(4, list.size());
    }

    @Test
    void test10() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "date": {
                                    "in": ["2024-05-01T00:00:00", "2024-04-25T00:00:00"]
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(2, list.size());
    }

    @Test
    void test11() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "age": {
                                    "!in": [10, 11, 12]
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(4, list.size());
    }

    @Test
    void test12() throws IOException {
        JsonNode node = treeWalker.convertJSONToNode("""
                {
                    "AND": [
                            {
                                "enrolled": {
                                    "eq": true
                                }
                            }
                           ]
                    }
                }
                """);
        List<Person> list = treeWalker.getAllPersons(node);

        assertEquals(2, list.size());
    }
}