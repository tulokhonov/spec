package com.example.spec.service;

import com.example.spec.service.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TreeWalkerTest {

    @Autowired
    private TreeWalker treeWalker;

    @Sql("/data.sql")
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
                                    }
                                ]
                            }
                        ]
                    }
                }
                """);
        List<Person> list = treeWalker.walkTree(node);

        assertEquals(2, list.size());
    }
}