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
package org.phenotips.data.permissions.rest;

import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RelatedResources;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient record's owners, identified by patient record's internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Role
@Path("/patients/{patient-id}/permissions/owner")
@Relation("https://phenotips.org/rel/owner")
@ParentResource(PermissionsResource.class)
@RelatedResources(PatientResource.class)
public interface OwnerResource
{
    /**
     * Retrieves the owner of a patient record - which is found by the passed in patient identifier - if the indicated
     * patient record doesn't exist, or if the user sending the request doesn't have the right to view the target
     * patient record, an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @return REST representation of an owner of a patient record
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    OwnerRepresentation getOwner(@PathParam("patient-id") String patientId);

    /**
     * Updates the owner of a patient record - identified by `patientId` - with the owner specified in JSON. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to edit the
     * target patient record, no change is performed and an error is returned.
     *
     * @param owner an owner representation, only the {@code id} property is required, with a value of either a fully
     *            qualified username or a plain username (eg. {@code xwiki:XWiki.username} or {@code username})
     * @param patientId identifier of the patient whose owner should be changed
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setOwner(OwnerRepresentation owner, @PathParam("patient-id") String patientId);

    /**
     * Updates the owner of a patient record - identified by `patientId` - with the owner specified in JSON. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to edit the
     * target patient record, no change is performed and an error is returned. The request must contain an "owner"
     * field.
     *
     * @param patientId identifier of the patient, whose owner should be changed
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response setOwner(@PathParam("patient-id") String patientId);
}
