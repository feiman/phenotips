/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.Constants;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.vocabularies.rest.CategoryResource;
import org.phenotips.vocabularies.rest.CategoryTermSuggestionsResource;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Default implementation of {@link VocabularyResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyResource")
@Singleton
@Unstable
public class DefaultVocabularyResource extends XWikiResource implements VocabularyResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager users;

    @Inject
    @Named("default")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public org.phenotips.vocabularies.rest.model.Vocabulary getVocabulary(String vocabularyId)
    {
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return this.objectFactory.createLinkedVocabularyRepresentation(vocabulary, getVocabularyLinks(),
            this::getCategoriesForVocabulary);
    }

    /**
     * Returns a list of {@link org.phenotips.vocabularies.rest.model.Category} for a specified vocabulary.
     *
     * @param vocab a {@link Vocabulary} of interest
     * @return a list of {@link org.phenotips.vocabularies.rest.model.Category} associated with the {@code vocab}
     */
    private List<org.phenotips.vocabularies.rest.model.Category> getCategoriesForVocabulary(final Vocabulary vocab)
    {
        final Collection<String> categories = vocab.getSupportedCategories();
        return this.objectFactory.createCategoriesRepresentation(categories, getCategoryLinks(), null);
    }

    /**
     * Returns the autolinker with all resources common to all vocabularies set.
     *
     * @return an {@link Autolinker}
     */
    private Autolinker getVocabularyLinks()
    {
        return this.autolinker.get()
            .forResource(getClass(), this.uriInfo)
            .withGrantedRight(userIsAdmin() ? Right.ADMIN : Right.VIEW);
    }

    /**
     * Returns the autolinker with all resources common to all categories set.
     *
     * @return an {@link Autolinker}
     */
    private Autolinker getCategoryLinks()
    {
        return this.autolinker.get()
            .forSecondaryResource(CategoryResource.class, this.uriInfo)
            .withActionableResources(CategoryTermSuggestionsResource.class)
            .withGrantedRight(userIsAdmin() ? Right.ADMIN : Right.VIEW);
    }

    @Override
    public Response reindex(String vocabularyId, String url)
    {
        // Check permissions, the user must have admin rights on the entire wiki
        if (!this.userIsAdmin()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Response result;
        try {
            int reindexStatus = vocabulary.reindex(url);

            if (reindexStatus == 0) {
                result = Response.ok().build();
            } else if (reindexStatus == 1) {
                result = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                result = Response.status(Response.Status.BAD_REQUEST).build();
            }
        } catch (UnsupportedOperationException e) {
            result = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        return result;
    }

    private boolean userIsAdmin()
    {
        User user = this.users.getCurrentUser();
        return this.authorizationService.hasAccess(user, Right.ADMIN,
            this.resolver.resolve(Constants.XWIKI_SPACE_REFERENCE));
    }
}
