## Edit Existing Metadata Object

Use this workflow when the user asks to add or change attributes on an existing
metadata object, for example "доработать справочник Клиенты" or "добавить
реквизиты в документ". Do not recreate or reattach the top-level object.

### Hard rule

Do not answer "внесите изменения самостоятельно" for supported metadata
children such as `CatalogAttribute`, `DocumentAttribute`,
`TabularSectionAttribute`, `InformationRegisterAttribute`,
`InformationRegisterDimension`, or `InformationRegisterResource`. If a first
JShell attempt fails, inspect the exact error, fix the snippet, and retry.

### Canonical imports

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McoreFactory;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.TabularSectionAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

`manual_ids` do not execute imports. Import classes in the same JShell session
or fully qualify them.

### Primitive TypeItem helper

For primitive types, `typeProvider.getProxy(...)` is usually enough for
`STRING`, `NUMBER`, and `DATE`, but in some EDT/JShell sessions it can return
`null` for `IEObjectTypeNames.BOOLEAN`. Never pass a null `TypeItem` into
`TypeDescriptionBuilder.addType(...)`; it raises:

```text
java.lang.IllegalArgumentException: The 'no null' constraint is violated
```

Use this helper inside the BM task:

```java
java.util.function.Function<String, TypeItem> primitiveType = (typeName) -> {
    TypeItem item = (TypeItem)typeProvider.getProxy(typeName);
    if (item == null) {
        try {
            item = (TypeItem)typeProvider.createProxy(typeName);
        } catch (IllegalArgumentException e) {
            item = null;
        }
    }
    if (item == null) {
        item = McoreFactory.eINSTANCE.createType();
        item.setName(typeName);
        item.setNameRu(typeName);
    }
    return item;
};
```

Use `primitiveType.apply(IEObjectTypeNames.BOOLEAN)` for boolean attributes.
Keep `typeProvider.createProxy(...)` and `McoreFactory.eINSTANCE.createType()`
only as primitive fallbacks. Do not use them for concrete metadata references
such as `CatalogRef.X` or `EnumRef.X`.

### Example: edit existing Catalog and add several attributes

This pattern is restartable: it skips attributes that already exist and fixes
an existing attribute only if its type is empty.

```java
{
    IProject project = workspaceRoot.getProject("MyProject");
    IV8Project v8project = projectManager.getProject(project);
    IBmModel bmModel = modelManager.getModel(project);
    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

    globalContext.execute(new AbstractBmTask<Void>("Edit catalog attributes") {
        @Override
        public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
            Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Clients");
            if (catalog == null) {
                throw new IllegalStateException("Missing target catalog: Catalog.Clients");
            }

            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
            java.util.function.Function<String, TypeItem> primitiveType = (typeName) -> {
                TypeItem item = (TypeItem)typeProvider.getProxy(typeName);
                if (item == null) {
                    try {
                        item = (TypeItem)typeProvider.createProxy(typeName);
                    } catch (IllegalArgumentException e) {
                        item = null;
                    }
                }
                if (item == null) {
                    item = McoreFactory.eINSTANCE.createType();
                    item.setName(typeName);
                    item.setNameRu(typeName);
                }
                return item;
            };

            TypeItem stringType = primitiveType.apply(IEObjectTypeNames.STRING);
            TypeItem booleanType = primitiveType.apply(IEObjectTypeNames.BOOLEAN);

            com._1c.g5.v8.dt.metadata.mdclass.Enum channelEnum =
                (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn("Enum.PreferredChannels");
            if (channelEnum == null) {
                throw new IllegalStateException("Missing dependency: Enum.PreferredChannels");
            }
            TypeItem channelRef = MdProducedTypesUtil.getProducedType(
                channelEnum, MdTypePackage.Literals.MD_REF_TYPE);

            addOrRepairCatalogAttribute(catalog, "PreferredChannel",
                new TypeDescriptionBuilder().addType(channelRef).build());
            addOrRepairCatalogAttribute(catalog, "FavoriteGenre",
                new TypeDescriptionBuilder().addType(stringType).setStringQualifiers(100, false).build());
            addOrRepairCatalogAttribute(catalog, "MailingConsent",
                new TypeDescriptionBuilder().addType(booleanType).build());

            return null;
        }

        private void addOrRepairCatalogAttribute(Catalog catalog, String name, TypeDescription type) {
            CatalogAttribute attr = catalog.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
            if (attr == null) {
                attr = mdFactory.createCatalogAttribute();
                attr.setName(name);
                attr.setUuid(UUID.randomUUID());
                attr.setType(type);
                catalog.getAttributes().add(attr);
            } else if (attr.getType() == null || attr.getType().getTypes().isEmpty()) {
                attr.setType(type);
            }
        }
    });
}
```

### Reference type rule

For concrete metadata references (`CatalogRef.X`, `EnumRef.X`,
`DocumentRef.X`), fetch the referenced top object inside the transaction and
resolve the type with:

```java
TypeItem refType = MdProducedTypesUtil.getProducedType(depObject, MdTypePackage.Literals.MD_REF_TYPE);
```

Do not use `typeProvider.getProxy("EnumRef.X")`,
`typeProvider.createProxy("EnumRef.X")`, a generic `IEObjectTypeNames.ENUM_REF`,
or `String` as a fallback.

### Required post-check

After editing metadata, call `GetMarkers` with `marker_type: "1c"` and `path`
to the changed top-level `.mdo`. For `Catalog.Clients` the path is:

```text
<projectRoot>/src/Catalogs/Clients/Clients.mdo
```

Then read the object back and print every changed child by name and type:

```java
for (CatalogAttribute a : catalog.getAttributes()) {
    String typeName = a.getType() == null || a.getType().getTypes().isEmpty()
        ? "NULL"
        : a.getType().getTypes().get(0).getName();
    System.out.println(a.getName() + " : " + typeName);
}
```
