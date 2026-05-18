## Safe Workflow: Edit InformationRegister

### Canonical imports for child edits

When editing register dimensions, resources, or attributes in JShell, either
use these imports explicitly or fully qualify the mdclass names. The `edt`
scope may not already have child classes imported.

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterResource;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
```

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Edit InformationRegister") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        InformationRegister informationRegister = (InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices");
        if (informationRegister != null) {
            informationRegister.setInformationRegisterPeriodicity(InformationRegisterPeriodicity.DAY);
            informationRegister.setUseStandardCommands(true);

            InformationRegisterAttribute comment = informationRegister.getAttributes().stream()
                .filter(a -> "Comment".equals(a.getName()))
                .findFirst()
                .orElse(null);
            if (comment == null) {
                comment = mdFactory.createInformationRegisterAttribute();
                comment.setName("Comment");
                comment.setUuid(UUID.randomUUID());

                IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription commentType = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .setStringQualifiers(100, false)
                    .build();

                comment.setType(commentType);
                informationRegister.getAttributes().add(comment);
            } else if (comment.getType() == null || comment.getType().getTypes().isEmpty()) {
                IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
                    .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
                TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
                TypeDescription commentType = new TypeDescriptionBuilder()
                    .addType(stringType)
                    .setStringQualifiers(100, false)
                    .build();
                comment.setType(commentType);
            }
        }
        return null;
    }
});
```

### Notes
- Load the existing object by FQN from the transaction
- Do not recreate or reattach the object
- Do not call attachTopObject() for an existing register. Add or remove child objects through the existing collections.
- Use `mdFactory.createInformationRegisterAttribute()` for register requisites and add them through `informationRegister.getAttributes()`.
- Use `mdFactory.createInformationRegisterDimension()` and `informationRegister.getDimensions()` for dimensions.
- Use `mdFactory.createInformationRegisterResource()` and `informationRegister.getResources()` for resources.
- Every new dimension, resource, or attribute must have `setUuid(...)` and a fresh non-empty `TypeDescription` before being added.
- Before adding a dimension, resource, or attribute, find an existing child by
  `getName()`. Repair the existing child when possible; do not add duplicates.
  After editing, read back the collection and verify each requested name exists
  exactly once.
- After the transaction completes, run `GetMarkers` with `marker_type: "1c"` and `path` to the changed register `.mdo`, then read the register back and verify the requested child collections by name.
