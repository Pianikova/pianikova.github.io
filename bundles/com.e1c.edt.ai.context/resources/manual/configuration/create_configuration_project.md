## Safe Workflow: Create New 1C Configuration Project

This workflow creates a minimal EDT 1C:Enterprise configuration project in the Eclipse workspace.

Critical rule: create required project files first, then enable V8 nature and Xtext builder. If V8 nature is enabled before `DT-INF/PROJECT.PMF`, `src/Configuration/Configuration.mdo`, and `.settings` exist, EDT lifecycle can start on a half-created project. The result may be visible in workspace but not resolvable through `projectManager`.

Hard stop: a plain Eclipse project is not a 1C configuration project. Do not create only `.project` and then try to make a `Configuration` object through BM. Until `.settings`, `DT-INF/PROJECT.PMF`, `src/Configuration/Configuration.mdo`, the V8 nature, and the Xtext builder exist, `projectManager.getProject(IProject)` can return `null`, and that is a failed configuration creation.

Never report success when the verification prints `V8Project is null`, `BM model is null`, or `Configuration object is not accessible`. Stop, inspect the project structure, and recreate or repair the project with the full workflow below.

Import rule: if explicit imports are needed, use the imports below exactly. In a fresh JShell session the `edt` scope may not already contain Eclipse resource classes such as `NullProgressMonitor` or `IProjectDescription`. Do not send the project creation snippet until these classes are imported or fully qualified.

```java
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
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
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmGlobalEditingContext;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
```

Use `com._1c.g5.v8.dt.core.platform.IV8Project`. Do not import `com._1c.g5.v8.dt.core.IV8Project`; that package does not contain `IV8Project`.

```java
String projectName = "MyNewConfiguration";
IProject projectHandle = workspaceRoot.getProject(projectName);
NullProgressMonitor monitor = new NullProgressMonitor();

if (projectHandle.exists()) {
    System.err.println("ERROR: Project already exists: " + projectName);
    // Stop here in JShell and choose another project name.
}

try {
    // Step 1: Create and open a plain Eclipse project.
    // Do not set V8 nature yet.
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    projectHandle.create(description, monitor);
    projectHandle.open(monitor);
    projectHandle.setDefaultCharset("UTF-8", monitor);

    // Step 2: Create folders EDT may read during early project lifecycle.
    IFolder settingsFolder = projectHandle.getFolder(".settings");
    if (!settingsFolder.exists()) {
        settingsFolder.create(false, true, monitor);
    }

    IFolder srcFolder = projectHandle.getFolder("src");
    if (!srcFolder.exists()) {
        srcFolder.create(false, true, monitor);
    }

    IFolder configFolder = srcFolder.getFolder("Configuration");
    if (!configFolder.exists()) {
        configFolder.create(false, true, monitor);
    }

    IFolder dtinfFolder = projectHandle.getFolder("DT-INF");
    if (!dtinfFolder.exists()) {
        dtinfFolder.create(false, true, monitor);
    }

    // Step 3: Create PROJECT.PMF. It is an OSGi-like manifest, not XML.
    IFile pmfFile = dtinfFolder.getFile("PROJECT.PMF");
    String pmfContent = "Manifest-Version: 1.0\nRuntime-Version: 8.3.24\n";
    if (!pmfFile.exists()) {
        pmfFile.create(new ByteArrayInputStream(pmfContent.getBytes("UTF-8")), true, monitor);
    }

    // Step 4: Create minimal Configuration.mdo with mdclass namespace.
    IFile configFile = configFolder.getFile("Configuration.mdo");
    String configContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mdclass:Configuration xmlns:mdclass=\"http://g5.1c.ru/v8/dt/metadata/mdclass\" uuid=\"" + UUID.randomUUID().toString() + "\">\n" +
        "  <name>Configuration</name>\n" +
        "  <synonym>\n" +
        "    <key>ru</key>\n" +
        "    <value>\u041a\u043e\u043d\u0444\u0438\u0433\u0443\u0440\u0430\u0446\u0438\u044f</value>\n" +
        "  </synonym>\n" +
        "  <defaultRunMode>ManagedApplication</defaultRunMode>\n" +
        "  <usePurposes>PersonalComputer</usePurposes>\n" +
        "  <usedMobileApplicationFunctionalities>\n" +
        "    <functionality>\n" +
        "      <use>true</use>\n" +
        "    </functionality>\n" +
        "  </usedMobileApplicationFunctionalities>\n" +
        "</mdclass:Configuration>";
    if (!configFile.exists()) {
        configFile.create(new ByteArrayInputStream(configContent.getBytes("UTF-8")), true, monitor);
    }

    projectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    // Step 5: Enable V8 nature and Xtext builder only after required files exist.
    description = projectHandle.getDescription();
    description.setNatureIds(new String[] {
        "org.eclipse.xtext.ui.shared.xtextNature",
        "com._1c.g5.v8.dt.core.V8ConfigurationNature"
    });

    ICommand command = description.newCommand();
    command.setBuilderName("org.eclipse.xtext.ui.shared.xtextBuilder");
    description.setBuildSpec(new ICommand[] { command });
    projectHandle.setDescription(description, IResource.FORCE, monitor);
    projectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    // Step 6: Wait for asynchronous EDT initialization.
    boolean initialized = false;
    System.out.println("Waiting for project initialization...");

    int maxAttempts = 40; // 40 attempts * 500ms = 20 seconds max
    for (int attempt = 1; attempt <= maxAttempts && !initialized; attempt++) {
        try {
            Thread.sleep(500);

            // In JShell use the IProject handle. projectManager.getProject(String) can return null
            // while project lifecycle caches are still settling.
            IV8Project v8project = projectManager.getProject(projectHandle);
            if (v8project == null) {
                continue;
            }

            IBmModel bmModel = modelManager.getModel(projectHandle);
            if (bmModel == null) {
                continue;
            }

            IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
            Configuration config = globalContext.execute(new AbstractBmTask<Configuration>("Check Configuration") {
                @Override
                public Configuration execute(IBmTransaction transaction, IProgressMonitor taskMonitor) {
                    return (Configuration)transaction.getTopObjectByFqn("Configuration");
                }
            });

            if (config != null) {
                initialized = true;
                System.out.println("Project initialized in " + (attempt * 500) + "ms");
                System.out.println("SUCCESS: V8 project created");
                System.out.println("Project name: " + projectHandle.getName());
                System.out.println("Version: " + v8project.getVersion());
                System.out.println("SUCCESS: BM model initialized");
                System.out.println("SUCCESS: Configuration object is accessible");
                System.out.println("Configuration name: " + config.getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Polling interrupted: " + e.getMessage());
            break;
        }
    }

    if (!initialized) {
        System.err.println("ERROR: Project initialization failed after " + maxAttempts + " attempts");
        System.err.println("Check trace.log and verify .settings, DT-INF/PROJECT.PMF, and src/Configuration/Configuration.mdo");
        // Stop here in JShell and inspect project logs before continuing.
    }
} catch (CoreException | UnsupportedEncodingException e) {
    System.err.println("ERROR creating project: " + e.getMessage());
    e.printStackTrace();
}
```

### Rules

- Treat the requested project name as an exact literal. Do not correct, lemmatize, translate, or substitute it. `Булочная` must stay `Булочная` everywhere.
- Create a plain Eclipse project first.
- Do not stop after `projectHandle.create(...)` / `projectHandle.open(...)`. That creates only an Eclipse container, not an EDT configuration project.
- In a fresh session, import or fully qualify `NullProgressMonitor`, `IProjectDescription`, `ResourcesPlugin`, `IFolder`, `IFile`, `IResource`, `ICommand`, `CoreException`, and `ByteArrayInputStream` before running the snippet. Missing these imports causes JShell `cannot find symbol` compilation errors before any project files are created.
- Create `.settings`, `DT-INF/PROJECT.PMF`, `src`, and `src/Configuration/Configuration.mdo` before enabling V8 nature.
- `PROJECT.PMF` must be manifest format, not XML.
- `Configuration.mdo` must use `http://g5.1c.ru/v8/dt/metadata/mdclass`.
- Enable both natures after required files exist: `org.eclipse.xtext.ui.shared.xtextNature`, `com._1c.g5.v8.dt.core.V8ConfigurationNature`.
- Add Xtext builder after required files exist: `org.eclipse.xtext.ui.shared.xtextBuilder`.
- During verification, use `projectManager.getProject(projectHandle)`, not `projectManager.getProject(projectName)`.
- Verify `IV8Project`, `IBmModel`, and top object `Configuration`.
- After creating a new configuration project, run `GetMarkers` with `marker_type: "1c"` for the project because project initialization touches project-level files and the root `Configuration.mdo`. This project-wide check is specific to project creation; for ordinary 1C metadata CRUD, use `GetMarkers` with `path` to each changed top-level `.mdo` and do not fix unrelated project-wide markers.

### Known failure signs

- Project appears in `GetProjects`, but `projectManager.getProject("Name")` returns `null`: use the `IProject` handle and wait for lifecycle initialization.
- Project exists on disk with only `.project`, no `DT-INF/PROJECT.PMF`, and no `src/Configuration/Configuration.mdo`: it is just a plain Eclipse project. Recreate or repair it with this full workflow before any metadata CRUD.
- `V8Project is null` in JShell output is a failure, even if the tool response description says "configuration created successfully".
- `NoSuchFileException` for `.settings`: create `.settings` before enabling V8 nature.
- `ProjectManifestException`: check `DT-INF/PROJECT.PMF` format and `Runtime-Version`.
- BM model exists but `Configuration` is not accessible: check `src/Configuration/Configuration.mdo` namespace and root element.
