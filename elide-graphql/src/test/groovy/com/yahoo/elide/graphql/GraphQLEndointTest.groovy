/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.common.collect.Sets
import com.yahoo.elide.Elide
import com.yahoo.elide.ElideSettingsBuilder
import com.yahoo.elide.core.EntityDictionary
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore
import com.yahoo.elide.resources.DefaultOpaqueUserFunction
import graphqlEndpointTestModels.Author
import graphqlEndpointTestModels.Book
import graphqlEndpointTestModels.security.UserChecks
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import java.security.Principal

/**
 * GraphQL endpoint tests tested against the in-memory store.
 */
class GraphQLEndointTest {
    GraphQLEndpoint endpoint
    SecurityContext user1 = Mockito.mock(SecurityContext)
    SecurityContext user2 = Mockito.mock(SecurityContext)

    @BeforeTest
    void setup() {
        Mockito.when(user1.getUserPrincipal()).thenReturn(new Principal() {
            @Override
            String getName() {
                return "1"
            }
        })
        Mockito.when(user2.getUserPrincipal()).thenReturn(new Principal() {
            @Override
            String getName() {
                return "2"
            }
        })
    }

    @BeforeMethod
    void setupTest() throws Exception {
        InMemoryDataStore inMemoryStore = new InMemoryDataStore(Book.class.getPackage())
        Map<String, Class> checkMappings = new HashMap<>()
        checkMappings[UserChecks.IS_USER_1] = UserChecks.IsUserId.One.class
        checkMappings[UserChecks.IS_USER_2] = UserChecks.IsUserId.Two.class
        Elide elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build())
        endpoint = new GraphQLEndpoint(elide, new DefaultOpaqueUserFunction() {
            @Override
            Object apply(SecurityContext securityContext) {
                return securityContext.getUserPrincipal()
            }
        })

        def tx = inMemoryStore.beginTransaction()

        // Initial data
        Book book1 = new Book()
        Author author1 = new Author()
        Author author2 = new Author()

        book1.setId(1L)
        book1.setTitle("My first book")
        book1.setAuthors(Sets.newHashSet(author1))

        author1.setId(1L)
        author1.setName("Ricky Carmichael")
        author1.setBooks(Sets.newHashSet(book1))

        author2.setId(2)
        author2.setName("The Silent Author")

        tx.createObject(book1, null)
        tx.createObject(author1, null)
        tx.createObject(author2, null)

        tx.save(book1, null)
        tx.save(author1, null)
        tx.save(author2, null)

        tx.commit(null)

        print inMemoryStore.typeIds
    }

    @Test
    void testValidFetch() {
        String graphQLRequest =
            '''
            {
              book {
                edges {
                  node {
                    id
                    title
                    authors {
                      edges {
                        node {
                          name
                        }
                      }
                    }
                  }
                }
              }
            }            
            '''
        String graphQLResponse =
                '''
                {
                  "errors": [],
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "title": "My first book",
                            "authors": {
                              "edges": [
                                {
                                  "node": {
                                    "name": "Ricky Carmichael"
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                '''
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, graphQLResponse)
    }

    static String graphQLRequestToJSON(String request) {
        JsonNode node = JsonNodeFactory.instance.objectNode()
        node.put("query", request)
        return node.toString()
    }

    static JsonNode extract200Response(Response response) {
        Assert.assertEquals(response.getStatus(), 200)
        ObjectMapper mapper = new ObjectMapper()
        return mapper.readTree((String) response.getEntity())
    }

    static assert200EqualBody(Response response, String expected) {
        ObjectMapper mapper = new ObjectMapper()
        JsonNode expectedNode = mapper.readTree(expected)
        JsonNode actualNode = extract200Response(response)
        Assert.assertEquals(actualNode, expectedNode)
    }
}
