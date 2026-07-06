## Safe Workflow: Source-backed Template

Use this scenario for templates whose body cannot be safely generated as a blank EMF object:

- `TEXT_DOCUMENT` (`Template.txt`)
- `HTML_DOCUMENT` (`Template.htmldoc`)
- `BINARY_DATA` (`Template.bin`)
- `GEOGRAPHICAL_SCHEMA` (`Template.geos`)
- `GRAPHICAL_SCHEMA` (`Template.scheme`)
- `DATA_COMPOSITION_APPEARANCE_TEMPLATE` (`Template.dcsat`)
- `ADD_IN` (`Template.addin`)

### First action

If the user did not provide the actual source body/file, stop immediately:

- do not call JShell;
- do not call JShellReflection;
- do not create `Template` or `CommonTemplate` metadata;
- do not create a metadata-only stub;
- do not create empty body files with `Write`;
- ask for the required source file/content. Do not suggest or create a metadata-only stub unless the
  user's prompt literally says `metadata-only stub`, `только метаданные`, or `заглушка без файла`.

### Required clarification examples

- Text document: ask for the text content or path to a `.txt` source file.
- HTML document: ask for the HTML content or path to an `.htmldoc`/HTML source file.
- Binary data: ask for the binary source file path.
- Geographical schema: ask for the `.geos` source file.
- Graphical schema: ask for the `.scheme` source file.
- DCS appearance template: ask for the `.dcsat` source file.
- External component: ask for the `.addin` source file.

### When source is provided

Create/register the metadata with the correct `TemplateType` only after source content is available
or the user explicitly requested a metadata-only stub using those words. Use `create_common_template` for a shared
`CommonTemplate`, or `create_object_template` for an owner template. Then attach/copy the real body
according to the source format and validate the `.mdo` plus body file.

See `template_type_matrix` for exact `TemplateType`, `.mdo` value, and `Template.<ext>` mapping.

### CommonTemplate with source file: exact JShell path

Do not create a Task/design handoff. Use JShell directly.

1. Create the `CommonTemplate` metadata inside BM transaction.
2. Copy the real source file to `src/CommonTemplates/<Name>/Template.<ext>` with
   Eclipse EFS and `IFile.create(...)`.
3. Refresh the Eclipse project.
4. Run `GetMarkers` on `src/CommonTemplates/<Name>/<Name>.mdo` and verify body exists.

```java
import java.util.UUID;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.metadata.mdclass.CommonTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
```

```java
IProject project = workspaceRoot.getProject("<ProjectName>");
String templateName = "<TemplateName>";
TemplateType templateType = TemplateType.BINARY_DATA; // replace with the requested source-backed type
String bodyFileName = "Template.bin";                 // from template_type_matrix
String sourcePathString = "<absolute source path>";

IFileSystem fileSystem = EFS.getLocalFileSystem();
IFileStore sourceStore = fileSystem.getStore(new Path(sourcePathString));
if (!sourceStore.fetchInfo().exists()) {
    throw new IllegalStateException("Source file not found: " + sourcePathString);
}

IBmModel bmModel = modelManager.getModel(project);
if (bmModel == null) {
    throw new IllegalStateException("BM model is not available: " + project.getName());
}
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create source-backed common template") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        if (configuration == null) {
            throw new IllegalStateException("Missing Configuration top object");
        }

        String fqn = "CommonTemplate." + templateName;
        if (transaction.getTopObjectByFqn(fqn) != null) {
            throw new IllegalStateException("CommonTemplate already exists: " + fqn);
        }

        CommonTemplate template = mdFactory.createCommonTemplate();
        template.setName(templateName);
        template.getSynonym().put("ru", templateName);
        template.setTemplateType(templateType);
        template.setUuid(UUID.randomUUID());
        configuration.getCommonTemplates().add(template);

        String templateFqn = fqnGenerator.generateStandaloneObjectFqn(
            template.eClass(), template.getName()).toString();
        transaction.attachTopObject((IBmObject)template, templateFqn);
        return null;
    }
});

IFolder folder = project.getFolder("src/CommonTemplates/" + templateName);
if (!folder.exists()) {
    folder.create(true, true, new NullProgressMonitor());
}

IFile targetFile = project.getFile("src/CommonTemplates/" + templateName + "/" + bodyFileName);
java.io.InputStream sourceStream = sourceStore.openInputStream(0, new NullProgressMonitor());
try {
    if (targetFile.exists()) {
        targetFile.setContents(sourceStream, IResource.FORCE, new NullProgressMonitor());
    } else {
        targetFile.create(sourceStream, IResource.FORCE, new NullProgressMonitor());
    }
} finally {
    sourceStream.close();
}
project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

System.out.println("MDO exists: " +
    project.getFile("src/CommonTemplates/" + templateName + "/" + templateName + ".mdo").exists());
System.out.println("Body exists: " +
    project.getFile("src/CommonTemplates/" + templateName + "/" + bodyFileName).exists());
```

For `HTML_DOCUMENT`, use `TemplateType.HTML_DOCUMENT` and `Template.htmldoc`.
For `TEXT_DOCUMENT`, use `TemplateType.TEXT_DOCUMENT` and `Template.txt`.
For `BINARY_DATA`, use `TemplateType.BINARY_DATA` and `Template.bin`.
For `GEOGRAPHICAL_SCHEMA`, use `TemplateType.GEOGRAPHICAL_SCHEMA` and `Template.geos`.
For `GRAPHICAL_SCHEMA`, use `TemplateType.GRAPHICAL_SCHEMA` and `Template.scheme`.
For `DATA_COMPOSITION_APPEARANCE_TEMPLATE`, use
`TemplateType.DATA_COMPOSITION_APPEARANCE_TEMPLATE` and `Template.dcsat`.
For `ADD_IN`, use `TemplateType.ADD_IN` and `Template.addin`.
