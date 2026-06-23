# JShell EDT Canonical Imports

Use this card before writing JShell snippets for EDT metadata APIs. It prevents common package and method-name mistakes found in logs.

## Required Imports

The `edt` JShell scope pre-imports the common safe EDT classes. Do not add
wildcard imports. Add only explicit imports for scenario-specific classes that
are not already available in the session.

Passing `manual_ids` to a JShell tool call does not execute the imports shown
in that manual card. If a snippet uses classes such as `DocumentAttribute`,
`RealTimePosting`, `InformationRegisterAttribute`, `MdProducedTypesUtil`, or
`MdTypePackage`, import them in the same JShell session first or use their
fully-qualified names directly in the snippet.

```java
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McoreFactory;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentJournal;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumerator;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumberPeriodicity;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentNumberType;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.HTTPService;
import com._1c.g5.v8.dt.metadata.mdclass.WebService;
import com._1c.g5.v8.dt.metadata.mdclass.WSReference;
import com._1c.g5.v8.dt.metadata.mdclass.IntegrationService;
import com._1c.g5.v8.dt.metadata.mdclass.XDTOPackage;
import com._1c.g5.v8.dt.metadata.mdclass.RealTimePosting;
import com._1c.g5.v8.dt.metadata.mdclass.Sequence;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.metadata.mdclass.TabularSectionAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.EnumValue;
import com._1c.g5.v8.dt.metadata.mdclass.HierarchyType;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
```

For EDT enum metadata use the fully-qualified class name
`com._1c.g5.v8.dt.metadata.mdclass.Enum`. Do not import it and do not write the
simple name `Enum`, because it conflicts with `java.lang.Enum` in JShell.

## Do Not Use These Packages

- `com._1c.g5.v8.dt.bm.integration.AbstractBmTask` is wrong. Use `com._1c.g5.v8.bm.integration.AbstractBmTask`.
- `com._1c.g5.v8.dt.bm.integration.IBmTransaction` and `com._1c.g5.v8.bm.integration.IBmTransaction` are wrong. Use `com._1c.g5.v8.bm.core.IBmTransaction`.
- `com._1c.g5.v8.bm.integration.IProgressMonitor` is wrong. Use `org.eclipse.core.runtime.IProgressMonitor`.
- `com._1c.g5.v8.dt.mcore.IEObjectProvider` is wrong. Use `com._1c.g5.v8.dt.platform.IEObjectProvider`.
- `com._1c.g5.v8.dt.mcore.IEObjectTypeNames` is wrong. Use `com._1c.g5.v8.dt.platform.IEObjectTypeNames`.
- `com._1c.g5.v8.dt.mcore.TypeDescriptionBuilder` is wrong. Use `com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder`.
- `com._1c.g5.v8.dt.metadata.md.IEObjectTypeNames` is wrong. Use `com._1c.g5.v8.dt.platform.IEObjectTypeNames`.
- `com._1c.g5.v8.dt.md.TypeDescriptionBuilder` is wrong. Use `com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder`.
- `com._1c.g5.v8.dt.metadata.mdtype.TypeDescriptionBuilder` is wrong. Use `com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder`.
- `MdProducedTypesUtil` is not in the default package. Import `com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil` or use that FQN.
- `MdTypePackage` is not in the default package. Import `com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage` or use that FQN.
- `com._1c.g5.v8.dt.core.IV8Project` is wrong. Use `com._1c.g5.v8.dt.core.platform.IV8Project`.
- `com._1c.g5.v8.dt.core.project.IV8Project` is wrong. Use `com._1c.g5.v8.dt.core.platform.IV8Project`.
- `com._1c.g5.v8.dt.core.IV8ProjectManager` is wrong. Use `com._1c.g5.v8.dt.core.platform.IV8ProjectManager`.
- `com._1c.g5.v8.dt.core.model.IBmModel` is wrong. Use `com._1c.g5.v8.bm.integration.IBmModel`.
- `com._1c.g5.v8.dt.bm.model.IBmModel` is wrong. Use `com._1c.g5.v8.bm.integration.IBmModel`.
- `com._1c.g5.v8.dt.bm.model.IBmGlobalEditingContext` is wrong. Use `com._1c.g5.v8.bm.integration.IBmGlobalEditingContext`.
- `com._1c.g5.v8.bm.model.IBmModel` is wrong. Use `com._1c.g5.v8.bm.integration.IBmModel`.
- `com._1c.g5.v8.bm.model.IBmGlobalEditingContext` is wrong. Use `com._1c.g5.v8.bm.integration.IBmGlobalEditingContext`.
- `com._1c.g5.v8.dt.bm.integration.IBmModel` / `...IBmGlobalEditingContext` are wrong (no `dt`). The whole `com._1c.g5.v8.dt.bm.integration` package does NOT exist. BM integration types live under `com._1c.g5.v8.bm.integration` (IBmModel, IBmGlobalEditingContext, AbstractBmTask); BM core types under `com._1c.g5.v8.bm.core` (IBmTransaction, IBmObject).
- `com._1c.g5.v8.dt.form.model.FormType` is wrong for generation. Use `com._1c.g5.v8.dt.form.generator.FormType`.
- `com._1c.g5.v8.dt.form.model.FormDataPath` is not the route for object attributes. Do not hand-build data paths for "show object attribute on form"; use `formGenerator.generateForm(...)`.

## Persistent JShell Scope

JShell keeps top-level variables between requests. For non-trivial EDT snippets,
wrap code in a block so repeated names like `project`, `service`, `result`,
`bmModel`, and `globalContext` do not collide with older snippets. Treat this
as mandatory for final verification/readback snippets because stale top-level
bindings can make the tool output look like an older check instead of the code
you just sent.

```java
{
    IProject project = workspaceRoot.getProject("DemoProject");
    IV8Project v8project = projectManager.getProject(project);
    // create/read/edit metadata here
}
```

If a snippet must define top-level values, use unique names. A repeated top-level declaration can produce confusing runtime errors such as `NoSuchFieldError` in later snippets.

## Stale JShell Output

If `std_out`, returned text, or the assistant-facing response plainly belongs
to a different request than the code just sent, do not trust the result and do
not continue metadata edits in that `repl_session_id`.

Observed example: a request that created `Catalog.Книги` returned
`Sequence already exists: Sequence.ПоследовательностьСкладскихДокументов`,
which was output from an older snippet. Treat this as JShell session state
corruption or stale execution. Create a fresh `jshellsession`, rerun the current
operation with explicit imports, then validate with `GetMarkers`.

## Printing Readback Results

`globalContext.execute(...)` can return a value, but JShell does not print that
return value automatically inside the MCP response. For read/verify snippets,
assign the result to a variable and call `System.out.println(result)`.

```java
String result = globalContext.execute(new AbstractBmTask<String>("Verify object") {
    @Override
    public String execute(IBmTransaction transaction, IProgressMonitor monitor) {
        XDTOPackage packageObject =
            (XDTOPackage)transaction.getTopObjectByFqn("XDTOPackage.CommonSchema");
        return packageObject == null ? "missing" : packageObject.getNamespace();
    }
});
System.out.println(result);
```

## Java Identifiers For Russian Metadata Names

1C metadata names and synonyms are data. Java variable names are code. Do not
copy a user-visible name with spaces, quotes, hyphens, punctuation, or mixed
words directly into a Java identifier.

```java
// Wrong: spaces in a Java variable name cause "';' expected"
Document Поступление Товаров = ...;

// Correct: keep the 1C name in setName(), use a compact Java variable
Document поступлениеТоваров = ...;
поступлениеТоваров.setName("ПоступлениеТоваров");
поступлениеТоваров.getSynonym().put("ru", "Поступление товаров");
```

When reading with `transaction.getTopObjectByFqn(...)`, cast the result to the
exact metadata class before adding it to typed collections. `getTopObjectByFqn`
returns `IBmObject`; `DocumentJournal.getRegisteredDocuments().add(...)` expects
`Document`, not a raw `IBmObject` or `var`.

```java
Document realization = (Document)transaction.getTopObjectByFqn("Document.РеализацияТоваров");
if (realization != null) {
    journal.getRegisteredDocuments().add(realization);
}
```

## Exact Method Names

- `HTTPService`: use `setRootURL(...)` and `getRootURL()`. Do not use `getRootUrl()`.
- `WebService`: use `setNamespace(...)` and `getNamespace()`. Do not use `getNamespaceName()`.
- `WSReference`: use `setLocationURL(...)` and `getLocationURL()` for the baseline endpoint.
- `XDTOPackage`: `setNamespace(...)` is required for a valid baseline object; use `getNamespace()` to verify.
- `DocumentNumerator`: use `setNumberType(DocumentNumberType.NUMBER)`, `setNumberLength(int)`, and `setNumberPeriodicity(DocumentNumberPeriodicity.NONPERIODICAL)` when numbering details are required.
- `Document`: use `setNumerator(DocumentNumerator)` to attach a document to a numerator; keep document number type, length, and periodicity aligned with the assigned numerator.
- `Sequence`: use `getDocuments().add(document)` to include participating documents. There is no `setDocuments(...)`.
- `DocumentJournal`: use `getRegisteredDocuments().add(document)` to include documents. There is no `getDocuments()` or `setDocuments(...)` for this relationship.
- `TypeItem`: use `getName()` when printing or verifying the resolved type
  name such as `CatalogRef.Клиенты` or `EnumRef.СтатусыКлиентов`. Do not call
  `getTypeId()`; that method is not available on EDT `TypeItem` in JShell.
  Do not call `getLinkedMdObject()` either; it is not available on EDT
  `TypeItem` in JShell. For concrete references, `getName()` already includes
  the referenced metadata name.
  Do not rely on `TypeItem.toString()` for final readback either: it prints an
  implementation identity like `TypeImpl@...`, not the business type name.
- Primitive `TypeItem` values must also be null-checked. If
  `typeProvider.getProxy(IEObjectTypeNames.BOOLEAN)` (or another primitive)
  returns `null`, use `typeProvider.createProxy(IEObjectTypeNames.BOOLEAN)` as
  a fallback and throw `IllegalStateException` if it is still `null`. Never pass
  a null `TypeItem` to `TypeDescriptionBuilder.addType(...)`; that causes
  `The 'no null' constraint is violated`.

## Reflection Workflow

If more than one EDT type, method, enum, or factory is unknown, call one `JShellReflection` request with the full `queries` list before writing JShell code. Prefer manual cards for known top-level metadata CRUD.
