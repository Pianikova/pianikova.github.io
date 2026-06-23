## Safe Workflow: Create WSReference

Use this scenario only when the user provides a real WSDL URL or enough context to derive it.

### First decision
- If the prompt has no concrete WSDL URL, do not call JShell and do not create metadata.
- In that case, final answer must be a clarification question asking the user for the WSDL URL.
- If the prompt has a concrete WSDL URL, continue with the JShell create flow below.

### Hard rules
- Do not invent, synthesize, or "temporarily" use any URL. Forbidden examples include
  `example.com`, `placeholder`, `temp`, `localhost`, and made-up host names.
- `locationURL` is mandatory, but a non-empty fake URL still leaves the WSReference incomplete.
- If `GetMarkers` returns SU22 ("не найдено wsdl описания"), do not report success. Tell the user
  that a real/imported WSDL description is required, or update the object with the real URL when it
  is known.
- Create metadata only inside `bmModel.getGlobalContext().execute(new AbstractBmTask<...>(){...})`.
- Set UUID before attach, attach the top object with `fqnGenerator`, and add it to
  `Configuration.getWsReferences()`.
- After JShell, always run `GetMarkers` on
  `src/WSReferences/<Name>/<Name>.mdo` and inspect warnings as well as errors.

### Example when the prompt includes the WSDL URL

Use the actual URL from the user prompt. If there is no URL, stop before JShell and ask for it.

```java
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.WSReference;
```

```java
IProject project = workspaceRoot.getProject("MyProject");
String referenceName = "ExternalSoapService";
String wsdlUrl = wsdlUrlFromUserPrompt;

IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create WSReference") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        WSReference reference = mdFactory.createWSReference();
        reference.setName(referenceName);
        reference.setUuid(UUID.randomUUID());
        reference.setLocationURL(wsdlUrl);

        String fqn = fqnGenerator.generateStandaloneObjectFqn(reference.eClass(), reference.getName()).toString();
        transaction.attachTopObject((IBmObject)reference, fqn);
        configuration.getWsReferences().add(reference);
        return null;
    }
});
```

### Required post-check

Run `GetMarkers` with `marker_type: "1c"` for the created `.mdo` file.
Treat SU22 as incomplete unless the user explicitly requested a metadata stub and accepted that no
WSDL description can be resolved yet.
