/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import graphqlEndpointTestModels.security.UserChecks;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

@Include(rootLevel = true)
@Entity
@CreatePermission(expression = UserChecks.IS_USER_1)
@ReadPermission(expression = UserChecks.IS_USER_1 + " OR " + UserChecks.IS_USER_2)
@UpdatePermission(expression = UserChecks.IS_USER_1)
@DeletePermission(expression = UserChecks.IS_USER_1)
public class Book {
    long id;
    String title;
    Set<Author> authors = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ManyToMany
    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    @Transient
    @ComputedAttribute
    @ReadPermission(expression = UserChecks.IS_USER_1)
    public String getUser1SecretField() {
        return "this is a secret for user 1 only1";
    }

    public void setUser1SecretField() {
        // Do nothing
    }
}
