## Edit Document

Use this workflow for real 1C tasks like "доработать документ" or
"добавить реквизиты в существующий документ".

### Hard rules

- Use JShell/BM API. Do not edit `.mdo` text directly for metadata CRUD.
- Load the existing document with `transaction.getTopObjectByFqn("Document.<Name>")`.
- Do not recreate, reattach, or detach the top-level `Document`.
- Do not answer "внесите вручную" for `DocumentAttribute` changes.
- Do not create custom attributes named `Дата`, `Date`, `Номер`, `Number`,
  `Проведен`, or `Posted`; these are standard document fields/properties.
- Before adding a `DocumentAttribute`, search `document.getAttributes()` by
  `getName()`. If it already exists, repair the existing attribute instead of
  adding another one.
- A successful readback must prove that every requested attribute name exists
  exactly once. `GetMarkers` can be clean even when business attributes are
  duplicated.

### Imports

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McoreFactory;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.DocumentAttribute;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

### Full example: add or repair document attributes

This pattern is restartable when attributes are missing or have empty types.
It does not add duplicates.

```java
{
    IProject project = workspaceRoot.getProject("AI_КнижнаяCRM_2026-05-15");
    IV8Project v8project = projectManager.getProject(project);
    IBmModel bmModel = modelManager.getModel(project);
    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

    globalContext.execute(new AbstractBmTask<Void>("Edit Document.ОбращениеКлиента") {
        @Override
        public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
            Document document = (Document)transaction.getTopObjectByFqn("Document.ОбращениеКлиента");
            if (document == null) {
                throw new IllegalStateException("Missing target document: Document.ОбращениеКлиента");
            }

            IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());

            TypeItem booleanType = builtInType(typeProvider, IEObjectTypeNames.BOOLEAN);
            TypeItem dateType = builtInType(typeProvider, IEObjectTypeNames.DATE);
            TypeItem stringType = builtInType(typeProvider, IEObjectTypeNames.STRING);

            putAttr(document, "ТребуетсяОбратныйЗвонок", "Требуется обратный звонок",
                new TypeDescriptionBuilder().addType(booleanType).build());
            putAttr(document, "ПлановаяДатаОтвета", "Плановая дата ответа",
                new TypeDescriptionBuilder().addType(dateType).build());
            putAttr(document, "ОтветственныйКомментарий", "Комментарий ответственного",
                new TypeDescriptionBuilder()
                    .addType(stringType)
                    .setStringQualifiers(100, false)
                    .build());

            assertSingleAttribute(document, "ТребуетсяОбратныйЗвонок");
            assertSingleAttribute(document, "ПлановаяДатаОтвета");
            assertSingleAttribute(document, "ОтветственныйКомментарий");
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

        private void putAttr(Document document, String name, String synonymRu, TypeDescription type) {
            if (type == null || type.getTypes().isEmpty() || type.getTypes().get(0) == null) {
                throw new IllegalStateException("Empty TypeDescription for Document."
                    + document.getName() + "." + name);
            }

            DocumentAttribute attr = null;
            int count = 0;
            for (DocumentAttribute candidate : document.getAttributes()) {
                if (name.equals(candidate.getName())) {
                    count++;
                    if (attr == null) {
                        attr = candidate;
                    }
                }
            }
            if (count > 1) {
                throw new IllegalStateException("Duplicate DocumentAttribute found: "
                    + document.getName() + "." + name + ". Run cleanup before adding more attributes.");
            }

            if (attr == null) {
                attr = mdFactory.createDocumentAttribute();
                attr.setName(name);
                attr.setUuid(UUID.randomUUID());
                attr.getSynonym().put("ru", synonymRu);
                attr.setType(type);
                document.getAttributes().add(attr);
            } else if (attr.getType() == null || attr.getType().getTypes().isEmpty()) {
                attr.setType(type);
            }
        }

        private void assertSingleAttribute(Document document, String name) {
            int count = 0;
            for (DocumentAttribute attr : document.getAttributes()) {
                if (name.equals(attr.getName())) {
                    count++;
                    if (attr.getType() == null || attr.getType().getTypes().isEmpty()) {
                        throw new IllegalStateException("Attribute has empty type: "
                            + document.getName() + "." + name);
                    }
                }
            }
            if (count != 1) {
                throw new IllegalStateException("Expected exactly one DocumentAttribute "
                    + document.getName() + "." + name + ", actual count: " + count);
            }
        }
    });
}
```

### Cleanup duplicate document attributes

Use this only when readback already shows duplicate attributes with the same
`getName()`. Keep the first child and delete extra children with `EcoreUtil`.

```java
{
    IProject project = workspaceRoot.getProject("AI_КнижнаяCRM_2026-05-15");
    IBmModel bmModel = modelManager.getModel(project);
    IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

    globalContext.execute(new AbstractBmTask<Void>("Cleanup duplicate Document attributes") {
        @Override
        public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
            Document document = (Document)transaction.getTopObjectByFqn("Document.ОбращениеКлиента");
            if (document == null) {
                throw new IllegalStateException("Missing target document: Document.ОбращениеКлиента");
            }
            removeDuplicateAttributes(document, "ТребуетсяОбратныйЗвонок");
            removeDuplicateAttributes(document, "ПлановаяДатаОтвета");
            removeDuplicateAttributes(document, "ОтветственныйКомментарий");
            return null;
        }

        private void removeDuplicateAttributes(Document document, String name) {
            boolean firstSeen = false;
            for (DocumentAttribute attr : new java.util.ArrayList<DocumentAttribute>(document.getAttributes())) {
                if (!name.equals(attr.getName())) {
                    continue;
                }
                if (!firstSeen) {
                    firstSeen = true;
                    continue;
                }
                EcoreUtil.delete(attr);
            }
        }
    });
}
```

### Post-check

Run `GetMarkers` for the parent document `.mdo`:

```text
<projectRoot>/src/Documents/ОбращениеКлиента/ОбращениеКлиента.mdo
```

Then read back `document.getAttributes()` and print count plus
`attr.getType().getTypes().get(0).getName()` for each changed attribute. Do not
call `TypeItem.getLinkedMdObject()`; use `getName()` for the business type name.
