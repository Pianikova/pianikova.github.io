## IV8ProjectManager

Resolves `IV8Project` from Eclipse project.

```java
IProject eclipseProject = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(eclipseProject);
if (v8project != null) {
    System.out.println("Project: " + v8project.getProject().getName());
    System.out.println("Version: " + v8project.getVersion());
}
```
