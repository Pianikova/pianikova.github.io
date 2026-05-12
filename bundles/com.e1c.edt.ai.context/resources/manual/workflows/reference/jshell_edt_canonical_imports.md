# JShell EDT Canonical Imports

Use this card before writing JShell snippets for EDT metadata APIs. It prevents common package and method-name mistakes found in logs.

## Required Imports

```java
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
import com._1c.g5.v8.dt.metadata.mdclass.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
```

## Do Not Use These Packages

- `com._1c.g5.v8.dt.bm.integration.AbstractBmTask` is wrong. Use `com._1c.g5.v8.bm.integration.AbstractBmTask`.
- `com._1c.g5.v8.dt.bm.integration.IBmTransaction` is wrong. Use `com._1c.g5.v8.bm.integration.IBmTransaction`.
- `com._1c.g5.v8.dt.mcore.IEObjectProvider` is wrong. Use `com._1c.g5.v8.dt.platform.IEObjectProvider`.
- `com._1c.g5.v8.dt.metadata.md.IEObjectTypeNames` is wrong. Use `com._1c.g5.v8.dt.platform.IEObjectTypeNames`.
- `com._1c.g5.v8.dt.md.TypeDescriptionBuilder` is wrong. Use `com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder`.
- `com._1c.g5.v8.dt.core.IV8Project` is wrong. Use `com._1c.g5.v8.dt.core.platform.IV8Project`.
- `com._1c.g5.v8.dt.core.IV8ProjectManager` is wrong. Use `com._1c.g5.v8.dt.core.platform.IV8ProjectManager`.

## Persistent JShell Scope

JShell keeps top-level variables between requests. For non-trivial EDT snippets, wrap code in a block so repeated names like `project`, `service`, `result`, `bmModel`, and `globalContext` do not collide with older snippets.

```java
{
    IProject project = workspaceRoot.getProject("DemoProject");
    IV8Project v8project = projectManager.getProject(project);
    // create/read/edit metadata here
}
```

If a snippet must define top-level values, use unique names. A repeated top-level declaration can produce confusing runtime errors such as `NoSuchFieldError` in later snippets.

## Exact Method Names

- `HTTPService`: use `setRootURL(...)` and `getRootURL()`. Do not use `getRootUrl()`.
- `WebService`: use `setNamespace(...)` and `getNamespace()`. Do not use `getNamespaceName()`.
- `XDTOPackage`: `setNamespace(...)` is required for a valid baseline object; use `getNamespace()` to verify.

## Reflection Workflow

If more than one EDT type, method, enum, or factory is unknown, call one `JShellReflection` request with the full `queries` list before writing JShell code. Prefer manual cards for known top-level metadata CRUD.
