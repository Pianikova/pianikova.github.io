## MdClassFactory

**Primary Purpose:** Factory for creating 1C metadata objects (catalogs, documents, registers, etc.).
**Use Case:** Create NEW metadata objects inside BM transactions.

### ⚠️ CRITICAL RESTRICTIONS:

1. **MUST be used ONLY inside BM transaction** (`AbstractBmTask.execute()` body)
2. **Do NOT use for editing existing objects** - use `getTopObjectByFqn()` instead
3. **Objects MUST have UUIDs set** - use manual assignment `object.setUuid(UUID.randomUUID())` for JShell (RECOMMENDED); avoid `modelFactory.fillDefaultReferences(object)` in JShell because it may timeout
4. **Do NOT use `attachTopObject()` for existing objects** - causes `BmFqnAlreadyInUseException`

### Supported Metadata Object Types:

| Type | Factory Method | FQN Prefix |
|------|----------------|------------|
| Catalog (Справочник) | `createCatalog()` | `Catalog.` |
| Document (Документ) | `createDocument()` | `Document.` |
| BusinessProcess (Бизнес-процесс) | `createBusinessProcess()` | `BusinessProcess.` |
| Task (Задача) | `createTask()` | `Task.` |
| Sequence (Последовательность) | `createSequence()` | `Sequence.` |
| DocumentJournal (ЖурналДокументов) | `createDocumentJournal()` | `DocumentJournal.` |
| DocumentNumerator (НумераторДокументов) | `createDocumentNumerator()` | `DocumentNumerator.` |
| DefinedType (ОпределяемыйТип) | `createDefinedType()` | `DefinedType.` |
| InformationRegister (РегистрСведений) | `createInformationRegister()` | `InformationRegister.` |
| AccumulationRegister (РегистрНакопления) | `createAccumulationRegister()` | `AccumulationRegister.` |
| AccountingRegister (РегистрБухгалтерии) | `createAccountingRegister()` | `AccountingRegister.` |
| CalculationRegister (РегистрРасчета) | `createCalculationRegister()` | `CalculationRegister.` |
| Enum (Перечисление) | `createEnum()` | `Enum.` |
| ChartOfCharacteristicTypes (ПланВидовХарактеристик) | `createChartOfCharacteristicTypes()` | `ChartOfCharacteristicTypes.` |
| ChartOfAccounts (ПланСчетов) | `createChartOfAccounts()` | `ChartOfAccounts.` |
| ChartOfCalculationTypes (ПланВидовРасчета) | `createChartOfCalculationTypes()` | `ChartOfCalculationTypes.` |
| Report (Отчет) | `createReport()` | `Report.` |
| DataProcessor (Обработка) | `createDataProcessor()` | `DataProcessor.` |
| CommonModule (ОбщийМодуль) | `createCommonModule()` | `CommonModule.` |
| Constant (Константа) | `createConstant()` | `Constant.` |
| CommonAttribute (ОбщийРеквизит) | `createCommonAttribute()` | N/A |
| ExchangePlan (ПланОбмена) | `createExchangePlan()` | `ExchangePlan.` |
| EventSubscription (ПодпискаНаСобытие) | `createEventSubscription()` | N/A |
| ScheduledJob (РегламентноеЗадание) | `createScheduledJob()` | N/A |
| FilterCriterion (КритерийОтбора) | `createFilterCriterion()` | N/A |
| FunctionalOption (ФункциональнаяОпция) | `createFunctionalOption()` | N/A |
| WSReference (WSСсылка) | `createWSReference()` | `WSReference.` |
| HTTPService (HTTPСервис) | `createHTTPService()` | `HTTPService.` |
| WebService (Web-сервис) | `createWebService()` | `WebService.` |
| IntegrationService (СервисИнтеграции) | `createIntegrationService()` | `IntegrationService.` |

### Supported Attribute/Section Types:

| Type | Factory Method | Parent Object |
|------|----------------|---------------|
| CatalogAttribute | `createCatalogAttribute()` | Catalog |
| DocumentAttribute | `createDocumentAttribute()` | Document |
| BusinessProcessAttribute | `createBusinessProcessAttribute()` | BusinessProcess |
| TaskAttribute | `createTaskAttribute()` | Task |
| RegisterAttribute | `createRegisterAttribute()` | Register |
| InformationRegisterDimension | `createInformationRegisterDimension()` | InformationRegister |
| InformationRegisterResource | `createInformationRegisterResource()` | InformationRegister |
| AccumulationRegisterDimension | `createAccumulationRegisterDimension()` | AccumulationRegister |
| AccumulationRegisterResource | `createAccumulationRegisterResource()` | AccumulationRegister |
| AccountingRegisterDimension | `createAccountingRegisterDimension()` | AccountingRegister |
| AccountingRegisterResource | `createAccountingRegisterResource()` | AccountingRegister |
| CalculationRegisterDimension | `createCalculationRegisterDimension()` | CalculationRegister |
| CalculationRegisterResource | `createCalculationRegisterResource()` | CalculationRegister |
| TabularSectionAttribute | `createTabularSectionAttribute()` | TabularSection |
| CatalogTabularSection | `createCatalogTabularSection()` | Catalog |
| DocumentTabularSection | `createDocumentTabularSection()` | Document |
| BusinessProcessTabularSection | `createBusinessProcessTabularSection()` | BusinessProcess |
| TaskTabularSection | `createTaskTabularSection()` | Task |
| BasicForm | `createBasicForm()` | Any metadata object |
| BasicCommand | `createBasicCommand()` | Any metadata object |
| Template | `createTemplate()` | Any metadata object |
| EnumValue | `createEnumValue()` | Enum |
| PredefinedItem | `createPredefinedItem()` | Catalog, ChartOfCharacteristicTypes |
| Method | `createMethod()` | CommonModule |
| Parameter | `createParameter()` | Method |
| Operation | `createOperation()` | WebService |
| Column | `createColumn()` | Cube, Table |
| DimensionTable | `createDimensionTable()` | Cube |
| Table | `createTable()` | Cube |
| Recalculation | `createRecalculation()` | CalculationRegister |
| RecalculationDimension | `createRecalculationDimension()` | Recalculation |

### Correct Usage Example:

```java
IV8Project v8project = projectManager.getProject(project);
// ✅ CORRECT: Creating a new catalog
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
catalog.getSynonym().put("ru", "Products");
catalog.setHierarchyType(HierarchyType.HIERARCHY_FOLDERS_AND_ITEMS);
catalog.setCodeLength(9);
catalog.setDescriptionLength(150);

// Add attribute
CatalogAttribute attribute = mdFactory.createCatalogAttribute();
attribute.setName("Article");
attribute.getSynonym().put("ru", "Article");
IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
TypeDescription attrType = new TypeDescriptionBuilder()
    .addType(stringType)
    .build();
attribute.setType(attrType);
catalog.getAttributes().add(attribute);

// CRITICAL: Set UUIDs manually (RECOMMENDED for JShell - avoids OSGi timeout)
catalog.setUuid(UUID.randomUUID());
attribute.setUuid(UUID.randomUUID());

// Generate FQN and attach to transaction
String fqn = fqnGenerator.generateStandaloneObjectFqn(catalog.eClass(), catalog.getName()).toString();
transaction.attachTopObject((IBmObject)catalog, fqn);

// Add to configuration
Configuration config = (Configuration)transaction.getTopObjectByFqn("Configuration");
config.getCatalogs().add(catalog);
```

### Common Mistakes:

**❌ WRONG #1: Using mdFactory to edit existing object**
```java
Catalog catalog = mdFactory.createCatalog(); // Creates NEW object
transaction.attachTopObject((IBmObject)catalog, "Catalog.Products"); // ❌ FQN already exists!
```

**❌ WRONG #2: Not setting UUIDs**
```java
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
transaction.attachTopObject((IBmObject)catalog, fqn);
// Error: SU45 - UUID required for all metadata objects
```

**❌ WRONG #3: Using mdFactory outside transaction**
```java
// ❌ This will fail!
Catalog catalog = mdFactory.createCatalog(); // Outside transaction
```

**✅ CORRECT: Edit existing object**
```java
// Get EXISTING object - do NOT use mdFactory
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
if (catalog != null) {
    catalog.setDescriptionLength(200); // Modify directly
}
```

### Available Public Methods:

{{$method-list:MdClassFactory}}

**Note:** In JShell, prefer `mdFactory` plus manual UUID assignment for new metadata objects.
Use `modelFactory` only when you specifically need its higher-level behavior and can tolerate possible OSGi timeout issues.