## Edit Catalog

Use this workflow for real 1C tasks like "доработать справочник Клиенты" or
"добавить реквизиты в существующий справочник".

### Hard rules

- Use JShell/BM API. Do not edit `.mdo` text directly for metadata CRUD.
- Do not invent helper classes such as `EnumTypeUtil`, `TypeItemUtil`, or
  `StringQualifiersUtil`.
- Do not call `v8project.getModel().createTypeDescription()`.
- Do not clone another attribute type and mutate the `Type.name` string.
- Do not answer "внесите вручную" for catalog attributes.
- Do not create custom attributes named `Код`, `Code`, `Наименование`, or
  `Description`; those are standard catalog fields.

### Imports

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
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

### Full example: add EnumRef, String, and Boolean attributes

This is restartable. It creates missing attributes and repairs an existing
attribute if its type is empty.

```java
{
    IProject project = workspaceRoot.getProject("AI_КнижнаяCRM_2026-05-15");
    IV8Project v8project = projectManager.getProject(project);
    IBmModel bmModel = modelManager.getModel(project);
    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

    globalContext.execute(new AbstractBmTask<Void>("Edit Catalog.Клиенты") {
        @Override
        public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
            Catalog clients = (Catalog)transaction.getTopObjectByFqn("Catalog.Клиенты");
            if (clients == null) {
                throw new IllegalStateException("Missing target catalog: Catalog.Клиенты");
            }

            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

            TypeItem stringType = builtInType(typeProvider, IEObjectTypeNames.STRING);
            TypeItem booleanType = builtInType(typeProvider, IEObjectTypeNames.BOOLEAN);

            com._1c.g5.v8.dt.metadata.mdclass.Enum channelEnum =
                (com._1c.g5.v8.dt.metadata.mdclass.Enum)transaction.getTopObjectByFqn(
                    "Enum.ПредпочтительныеКаналыСвязи");
            if (channelEnum == null) {
                throw new IllegalStateException("Missing dependency: Enum.ПредпочтительныеКаналыСвязи");
            }
            TypeItem channelRef = MdProducedTypesUtil.getProducedType(
                channelEnum, MdTypePackage.Literals.MD_REF_TYPE);

            putAttr(clients, "ПредпочтительныйКанал", "Предпочтительный канал",
                new TypeDescriptionBuilder().addType(channelRef).build());
            putAttr(clients, "ЛюбимыйЖанр", "Любимый жанр",
                new TypeDescriptionBuilder()
                    .addType(stringType)
                    .setStringQualifiers(100, false)
                    .build());
            putAttr(clients, "СогласиеНаРассылку", "Согласие на рассылку",
                new TypeDescriptionBuilder().addType(booleanType).build());

            return null;
        }

        private TypeItem builtInType(IEObjectProvider typeProvider, String typeName) {
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
        }

        private void putAttr(Catalog catalog, String name, String synonymRu, TypeDescription type) {
            if (type == null || type.getTypes().isEmpty() || type.getTypes().get(0) == null) {
                throw new IllegalStateException("Empty TypeDescription for Catalog." + catalog.getName() + "." + name);
            }
            CatalogAttribute attr = catalog.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
            if (attr == null) {
                attr = mdFactory.createCatalogAttribute();
                attr.setName(name);
                attr.setUuid(UUID.randomUUID());
                attr.getSynonym().put("ru", synonymRu);
                attr.setType(type);
                catalog.getAttributes().add(attr);
            } else if (attr.getType() == null || attr.getType().getTypes().isEmpty()) {
                attr.setType(type);
            }
        }
    });
}
```

### Post-check

Run `GetMarkers` for the parent catalog `.mdo`:

```text
<projectRoot>/src/Catalogs/Клиенты/Клиенты.mdo
```

Then read back `catalog.getAttributes()` and print
`attr.getType().getTypes().get(0).getName()` for each changed attribute.
Do not call `TypeItem.getLinkedMdObject()`; EDT `TypeItem` does not expose that
method in JShell. For a concrete enum reference, `getName()` already prints the
full business type, for example `EnumRef.ПредпочтительныеКаналыСвязи`.
