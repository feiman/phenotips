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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link LabeledExternalIdentifiersController} Component, only the overridden methods from
 * {@link AbstractComplexController} are tested here.
 */
public class LabeledExternalIdentifiersControllerTest
{
    private static final String IDENTIFIERS_STRING = "labeled_eids";

    private static final String CONTROLLER_NAME = IDENTIFIERS_STRING;

    private static final String INTERNAL_LABEL_KEY = "eidLabel";

    private static final String INTERNAL_VALUE_KEY = "eidValue";

    private static final String JSON_LABEL_KEY = "label";

    private static final String JSON_VALUE_KEY = "value";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Map<String, String>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Map<String, String>>>(
            LabeledExternalIdentifiersController.class);

    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    private List<BaseObject> identifiersXWikiObjects;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        final DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocument()).thenReturn(patientDocument);
        when(this.documentAccessBridge.getDocument(patientDocument)).thenReturn(this.doc);
        this.identifiersXWikiObjects = new LinkedList<>();
        when(this.doc.getXObjects(any(EntityReference.class))).thenReturn(this.identifiersXWikiObjects);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME,
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest())
                .getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        final List<String> result =
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertTrue(result.contains(INTERNAL_LABEL_KEY));
        Assert.assertTrue(result.contains(INTERNAL_VALUE_KEY));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest()).getBooleanFields()
                .isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest())
            .getCodeFields().isEmpty());
    }

    //-------------------------------------Test load()--------------------------------------//

    @Test
    public void loadWorks() throws Exception
    {
        for (int i = 0; i < 3; ++i) {
            final BaseObject identifier = mock(BaseObject.class);
            this.identifiersXWikiObjects.add(identifier);

            final BaseStringProperty labelString = mock(BaseStringProperty.class);
            when(labelString.getValue()).thenReturn("label" + i);
            when(identifier.getField(INTERNAL_LABEL_KEY)).thenReturn(labelString);

            final BaseStringProperty valueString = mock(BaseStringProperty.class);
            when(valueString.getValue()).thenReturn("value" + i);
            when(identifier.getField(INTERNAL_VALUE_KEY)).thenReturn(valueString);

            when(identifier.getFieldList()).thenReturn(Arrays.asList(labelString, valueString));
        }

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(3, result.size());
        for (int i = 0; i < 3; ++i) {
            Map<String, String> item = result.get(i);
            Assert.assertEquals("label" + i, item.get(INTERNAL_LABEL_KEY));
            Assert.assertEquals("value" + i, item.get(INTERNAL_VALUE_KEY));
        }
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        final Exception exception = new Exception();
        when(this.documentAccessBridge.getDocument(any(DocumentReference.class))).thenThrow(exception);

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen "
            + "error has occurred during controller loading ", exception.getMessage());
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveLabeledIdentifierClass() throws ComponentLookupException
    {
        when(this.doc.getXObjects(any(EntityReference.class))).thenReturn(null);

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenPatientHasEmptyLabeledIdentifierClass() throws ComponentLookupException
    {
        when(this.doc.getXObjects(any(EntityReference.class))).thenReturn(new LinkedList<BaseObject>());

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullFields() throws ComponentLookupException
    {
        final BaseObject obj = mock(BaseObject.class);
        when(obj.getField(anyString())).thenReturn(null);
        this.identifiersXWikiObjects.add(obj);

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullLabeledIdentifiers() throws ComponentLookupException
    {
        // Deleted objects appear as nulls in XWikiObjects list
        this.identifiersXWikiObjects.add(null);
        addLabeledIdentifierFields(INTERNAL_LABEL_KEY, new String[] { "MY ID" });
        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void checkLoadParsingOfLabelKey() throws ComponentLookupException
    {
        final String[] labels = new String[] { "A", "<!'>;", "two words", " ", "" };
        addLabeledIdentifierFields(INTERNAL_LABEL_KEY, labels);

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(labels[0], result.get(0).get(INTERNAL_LABEL_KEY));
        Assert.assertEquals(labels[1], result.get(1).get(INTERNAL_LABEL_KEY));
        Assert.assertEquals(labels[2], result.get(2).get(INTERNAL_LABEL_KEY));
        Assert.assertEquals(null, result.get(3).get(INTERNAL_LABEL_KEY));
        Assert.assertEquals(null, result.get(4).get(INTERNAL_LABEL_KEY));
    }

    @Test
    public void checkLoadParsingOfValueKey() throws ComponentLookupException
    {
        final String[] values = new String[] { "Hello world!", "<script></script>", "", "{{html}}" };
        addLabeledIdentifierFields(INTERNAL_VALUE_KEY, values);

        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(values[0], result.get(0).get(INTERNAL_VALUE_KEY));
        Assert.assertEquals(values[1], result.get(1).get(INTERNAL_VALUE_KEY));
        Assert.assertEquals(null, result.get(2).get(INTERNAL_VALUE_KEY));
        Assert.assertEquals(values[3], result.get(3).get(INTERNAL_VALUE_KEY));
    }

    //-----------------------------------Test writeJSON()-----------------------------------//

    @Test
    public void writeJSONReturnsNotNullWhenPatientHasNullDataForIdentifiers() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(IDENTIFIERS_STRING);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsNotNullWhenPatientHasEmptyDataForIdentifiers() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();
        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(IDENTIFIERS_STRING);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    /*
     * Tests that the passed JSON will not be affected by writeJSON in this controller if selected fields is not null,
     * and does not contain LabeledExternalIdentifierController.IDENTIFIERS_STRING
     */
    @Test
    public void writeJSONReturnsWhenSelectedFieldsDoesNotContainLabeledEidsEnabler() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();
        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        // selectedFields could contain any number of random strings; it should not affect the behavior in this case
        selectedFields.add("some_string");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONIgnoresItemsWhenIdentifierIsBlank() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();

        final Map<String, String> item = new LinkedHashMap<>();
        item.put(INTERNAL_LABEL_KEY, "");
        internalList.add(item);

        final Map<String, String> item2 = new LinkedHashMap<>();
        item2.put(INTERNAL_LABEL_KEY, null);
        internalList.add(item2);

        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(IDENTIFIERS_STRING);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertEquals(0, json.getJSONArray(CONTROLLER_NAME).length());
    }

    @Test
    public void writeJSONAddsContainerWithAllPickedValuesWhenSelectedFieldNamesIsNull() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();
        final String eidLabel = "identifierLabel";
        final String randomField = "randomField";
        final String fieldValue = "fieldValue";

        final Map<String, String> item = new LinkedHashMap<>();
        item.put(INTERNAL_LABEL_KEY, eidLabel);
        item.put(INTERNAL_VALUE_KEY, StringUtils.EMPTY);
        item.put(randomField, fieldValue);
        internalList.add(item);

        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertEquals(eidLabel, json.getJSONArray(CONTROLLER_NAME).getJSONObject(0).get(JSON_LABEL_KEY));
        Assert.assertEquals(StringUtils.EMPTY, json.getJSONArray(CONTROLLER_NAME).getJSONObject(0).get(JSON_VALUE_KEY));
        Assert.assertEquals(null, json.getJSONArray(CONTROLLER_NAME).optJSONObject(0).optString(randomField, null));
    }

    @Test
    public void writeJSONWorksCorrectly() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();

        final String identifierLabel = "IDENTIFIER";
        final String identifierValue = "VALUE";

        final Map<String, String> item = new LinkedHashMap<>();
        item.put(INTERNAL_LABEL_KEY, identifierLabel);
        item.put(INTERNAL_VALUE_KEY, identifierValue);
        internalList.add(item);

        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(IDENTIFIERS_STRING);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        final JSONObject result = json.getJSONArray(CONTROLLER_NAME).getJSONObject(0);
        Assert.assertEquals(identifierLabel, result.get(JSON_LABEL_KEY));
        Assert.assertEquals(identifierValue, result.get(JSON_VALUE_KEY));
        Assert.assertEquals(2, result.length()); // label, value
    }

    @Test
    public void writeJSONWorksCorrectlyIfLabelIsBlank() throws ComponentLookupException
    {
        final List<Map<String, String>> internalList = new LinkedList<>();

        final String identifierLabel = "    ";
        final String identifierValue = "VALUE";

        final Map<String, String> item = new LinkedHashMap<>();
        item.put(INTERNAL_LABEL_KEY, identifierLabel);
        item.put(INTERNAL_VALUE_KEY, identifierValue);
        internalList.add(item);

        final PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        when((PatientData) this.patient.getData(CONTROLLER_NAME)).thenReturn(patientData);
        final JSONObject json = new JSONObject();
        final Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(IDENTIFIERS_STRING);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        final JSONArray result = json.getJSONArray(CONTROLLER_NAME);
        Assert.assertEquals(0, result.length());
    }

    //-----------------------------------Test readJSON()------------------------------------//

    @Test
    public void readWithNullJsonDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(null));
    }

    @Test
    public void readWithNoDataDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readWithWrongDataDoesNothing() throws ComponentLookupException
    {
        final JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, "Wrong data");
        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readWithWrongControllerReturnsNull() throws ComponentLookupException
    {
        final JSONObject json = new JSONObject();
        json.put("WrongController", "[]");
        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNull(result);
    }

    @Test
    public void readWithEmptyDataDoesNothing() throws ComponentLookupException
    {
        final JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, new JSONArray());
        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readWorksCorrectly() throws ComponentLookupException
    {
        final JSONArray data = new JSONArray();
        final JSONObject item = new JSONObject();
        item.put(JSON_LABEL_KEY, "LABEL1");
        item.put(JSON_VALUE_KEY, "value1");
        data.put(item);
        final JSONObject item2 = new JSONObject();
        item2.put(JSON_LABEL_KEY, "LABEL2");
        item2.put(JSON_VALUE_KEY, "value2");
        data.put(item2);
        final JSONObject item3 = new JSONObject();
        item3.put(JSON_LABEL_KEY, "");
        item3.put(JSON_VALUE_KEY, "value3");
        data.put(item3);
        final JSONObject item4 = new JSONObject();
        item4.put(JSON_LABEL_KEY, "LABEL4");
        item4.put(JSON_VALUE_KEY, "");
        data.put(item4);
        final JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        final PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.isIndexed());
        final Iterator<Map<String, String>> it = result.iterator();
        final Map<String, String> identifier1 = it.next();
        Assert.assertEquals("LABEL1", identifier1.get(INTERNAL_LABEL_KEY));
        Assert.assertEquals("value1", identifier1.get(INTERNAL_VALUE_KEY));
        final Map<String, String> identifier2 = it.next();
        Assert.assertEquals("LABEL2", identifier2.get(INTERNAL_LABEL_KEY));
        Assert.assertEquals("value2", identifier2.get(INTERNAL_VALUE_KEY));
        final Map<String, String> identifier4 = it.next();
        Assert.assertEquals("LABEL4", identifier4.get(INTERNAL_LABEL_KEY));
        Assert.assertEquals(StringUtils.EMPTY, identifier4.get(INTERNAL_VALUE_KEY));
    }

    //-------------------------------------Test save()--------------------------------------//

    @Test
    public void saveWithNoDataDoesNothing() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithWrongTypeOfDataDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(new SimpleValuePatientData<Object>("a", "b"));
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataClearsIdentifiers() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = xcontextProvider.get();
        when(context.getWiki()).thenReturn(mock(XWiki.class));
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        verify(this.doc).removeXObjects(LabeledExternalIdentifiersController.IDENTIFIER_CLASS_REFERENCE);

        Mockito.verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveUpdatesIdentifiers() throws ComponentLookupException, XWikiException
    {
        final List<Map<String, String>> data = new LinkedList<>();
        final Map<String, String> item = new HashMap<>();
        item.put(INTERNAL_LABEL_KEY, "LABEL1");
        item.put(INTERNAL_VALUE_KEY, "value1");
        data.add(item);
        final Map<String, String> item2 = new HashMap<>();
        item2.put(INTERNAL_LABEL_KEY, "LABEL2");
        data.add(item2);
        when(this.patient.<Map<String, String>>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = xcontextProvider.get();
        when(context.getWiki()).thenReturn(mock(XWiki.class));

        final BaseObject o1 = mock(BaseObject.class);
        final BaseObject o2 = mock(BaseObject.class);
        when(this.doc.newXObject(LabeledExternalIdentifiersController.IDENTIFIER_CLASS_REFERENCE, context))
            .thenReturn(o1, o2);

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.doc).removeXObjects(LabeledExternalIdentifiersController.IDENTIFIER_CLASS_REFERENCE);
        verify(o1).set(INTERNAL_LABEL_KEY, "LABEL1", context);
        verify(o1).set(INTERNAL_VALUE_KEY, "value1", context);
        verify(o2).set(INTERNAL_LABEL_KEY, "LABEL2", context);
        verify(o2, Mockito.never()).set(eq(INTERNAL_VALUE_KEY), anyString(), eq(context));
    }

    private void addLabeledIdentifierFields(final String key, final String[] fieldValues)
    {
        for (final String value : fieldValues) {
            final BaseObject obj = mock(BaseObject.class);
            final BaseStringProperty property = mock(BaseStringProperty.class);
            final List<String> list = new ArrayList<>();
            list.add(value);
            when(property.getValue()).thenReturn(value);
            when(obj.getField(key)).thenReturn(property);
            when(obj.getFieldList()).thenReturn(list);
            this.identifiersXWikiObjects.add(obj);
        }
    }
}
