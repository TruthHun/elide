/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.containers.NestedContainer;
import com.yahoo.elide.graphql.containers.PersistentResourceContainer;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import graphql.language.Field;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates GraphQL's DataFetchingEnvironment
 */
public class Environment {
    public final GraphQLRequestScope requestScope;
    public final Optional<List<String>> ids;
    public final Optional<String> sort;
    public final Optional<List<Map<String, Object>>> data;
    public final Optional<String> filters;
    public final Optional<String> offset;
    public final Optional<String> first;
    public final Object rawSource;

    public final PersistentResource parentResource;
    public final GraphQLType parentType;
    public final GraphQLType outputType;
    public final Field field;

    public Environment(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        requestScope = (GraphQLRequestScope) environment.getContext();

        filters = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FILTER));
        offset = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_AFTER));
        first = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FIRST));
        sort = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_SORT));

        parentType = environment.getParentType();

        outputType = environment.getFieldType();

        rawSource = environment.getSource();

        if (rawSource instanceof PersistentResourceContainer) {
            parentResource = ((PersistentResourceContainer) rawSource).getPersistentResource();
        } else if (rawSource instanceof PersistentResource) {
            parentResource = (PersistentResource) rawSource;
        } else {
            parentResource = null;
        }

        field = environment.getFields().get(0);

        this.ids = Optional.ofNullable((List<String>) args.get(ModelBuilder.ARGUMENT_IDS));

        List<Map<String, Object>> data = (List<Map<String, Object>>) args.get(ModelBuilder.ARGUMENT_DATA);
        this.data = Optional.ofNullable(data);
    }

    public boolean isRoot() {
        return parentResource == null && !(rawSource instanceof NestedContainer);
    }
}