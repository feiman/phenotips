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
package org.phenotips.data.internal.controller;

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides access to attached medical reports.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New controller in 1.4M1")
@Component(roles = { PatientDataController.class })
@Named("medicalReports")
@Singleton
public class MedicalReportsController implements PatientDataController<Attachment>
{
    private static final String DATA_NAME = "medical_reports";

    private static final String FIELD_NAME = "reports_history";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AttachmentAdapterFactory adapter;

    @Inject
    private Logger logger;

    @Override
    public PatientData<Attachment> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            // Getting the documents which are reports instead of just getting all attachments
            @SuppressWarnings("unchecked")
            List<String> reports = data.getListValue(FIELD_NAME);
            List<Attachment> result = new ArrayList<>(reports.size());

            for (String report : reports) {
                XWikiAttachment xattachment = doc.getAttachment(report);
                if (xattachment != null) {
                    result.add(this.adapter.fromXWikiAttachment(xattachment));
                }
            }

            return new IndexedPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        PatientData<Attachment> reports = patient.getData(getName());
        if (reports == null) {
            return;
        }
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE, true, this.contextProvider.get());

            List<String> result = new ArrayList<>(reports.size());

            for (Attachment report : reports) {
                XWikiAttachment xattachment = doc.getAttachment(report.getFilename());
                if (xattachment == null) {
                    xattachment = new XWikiAttachment(doc, report.getFilename());
                    doc.addAttachment(xattachment);
                }
                xattachment.setContent(report.getContent());
                DocumentReference author = report.getAuthorReference();
                if (author != null
                    && !this.contextProvider.get().getWiki().exists(author, this.contextProvider.get())) {
                    author = this.contextProvider.get().getUserReference();
                }
                xattachment.setAuthorReference(author);
                xattachment.setDate(report.getDate());
                xattachment.setFilesize((int) report.getFilesize());
                result.add(report.getFilename());
            }
            data.setDBStringListValue(FIELD_NAME, result);
        } catch (Exception ex) {
            this.logger.error("Failed to save attachment: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null
            && !(selectedFieldNames.contains(getName()) || selectedFieldNames.contains(FIELD_NAME))) {
            return;
        }

        PatientData<Attachment> reports = patient.getData(getName());
        JSONArray result = new JSONArray();
        if (reports == null || !reports.isIndexed() || reports.size() == 0) {
            if (selectedFieldNames != null) {
                json.put(DATA_NAME, result);
            }
            return;
        }

        for (Attachment report : reports) {
            result.put(report.toJSON());
        }
        json.put(DATA_NAME, result);
    }

    @Override
    public PatientData<Attachment> readJSON(JSONObject json)
    {
        if (!json.has(DATA_NAME) || json.optJSONArray(DATA_NAME) == null) {
            return null;
        }
        List<Attachment> result = new ArrayList<>();

        JSONArray reports = json.getJSONArray(DATA_NAME);
        for (Object report : reports) {
            result.add(this.adapter.fromJSON((JSONObject) report));
        }

        return new IndexedPatientData<>(getName(), result);
    }

    @Override
    public String getName()
    {
        return "medicalReports";
    }
}
