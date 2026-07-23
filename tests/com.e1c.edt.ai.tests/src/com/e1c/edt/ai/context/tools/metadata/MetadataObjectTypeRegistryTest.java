/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class MetadataObjectTypeRegistryTest
{
    @Test
    public void shouldMatchAllRsvMetadataTypes()
    {
        var expected = new LinkedHashSet<>(Arrays.asList(
            "Catalog", "Document", "InformationRegister", "AccumulationRegister", "AccountingRegister", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "CalculationRegister", "ChartOfAccounts", "ChartOfCharacteristicTypes", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "ChartOfCalculationTypes", "BusinessProcess", "Task", "Subsystem", "Role", "CommonModule", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "CommonForm", "CommonCommand", "CommonAttribute", "Constant", "HTTPService", "WebService", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "WSReference", "XDTOPackage", "Enum", "Report", "DataProcessor", "ExchangePlan", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "FunctionalOption", "FunctionalOptionsParameter", "DefinedType", "FilterCriterion", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "SessionParameter", "EventSubscription", "ScheduledJob", "DocumentJournal", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "DocumentNumerator", "Sequence", "Style", "StyleItem", "Language", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "SettingsStorage", "CommonPicture", "CommonTemplate", "CommandGroup", "Interface", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "ExternalDataSource", "IntegrationService", "Bot", "WebSocketClient", "PaletteColor", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "ExternalDataProcessor", "ExternalReport")); //$NON-NLS-1$ //$NON-NLS-2$

        var actual = MetadataObjectTypeRegistry.all().stream()
            .map(type -> type.name)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Assert.assertEquals(51, actual.size());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldExposeExactRsvFoldersAndExternalTypes()
    {
        Assert.assertEquals("ChartsOfCharacteristicTypes", //$NON-NLS-1$
            MetadataObjectTypeRegistry.get("ChartOfCharacteristicTypes").folder); //$NON-NLS-1$
        Assert.assertEquals("HTTPServices", MetadataObjectTypeRegistry.get("HTTPService").folder); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertEquals("WSReferences", MetadataObjectTypeRegistry.get("WSReference").folder); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertEquals("XDTOPackages", MetadataObjectTypeRegistry.get("XDTOPackage").folder); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertTrue(MetadataObjectTypeRegistry.get("ExternalDataProcessor").external); //$NON-NLS-1$
        Assert.assertTrue(MetadataObjectTypeRegistry.get("ExternalReport").external); //$NON-NLS-1$
        Assert.assertEquals(2, MetadataObjectTypeRegistry.all().stream().filter(type -> type.external).count());
    }

    @Test
    public void shouldResolveEveryRsvTypeAndConfigurationCollectionInEdtModel()
    {
        var errors = MetadataObjectTypeRegistry.validateEdtModel();

        Assert.assertTrue(errors.toString(), errors.isEmpty());
    }
}
