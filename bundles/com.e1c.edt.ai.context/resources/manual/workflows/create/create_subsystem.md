## Safe Workflow: Create Subsystem

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

globalContext.execute(new AbstractBmTask<Void>("Create subsystem") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        Subsystem subsystem = mdFactory.createSubsystem();
        subsystem.setName("MySubsystem");
        subsystem.getSynonym().put("ru", "МояПодсистема");
        subsystem.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(subsystem.eClass(), subsystem.getName()).toString();
        transaction.attachTopObject((IBmObject)subsystem, fqn);
        configuration.getSubsystems().add(subsystem);

        return null;
    }
});
```

### Important notes

- Subsystems are organizational objects that group related metadata objects together
- Subsystems can be nested (one subsystem can contain another)
- Subsystems do NOT have attributes, tabular sections, or modules
- Set UUID manually for reliable JShell execution (avoids OSGi timeout)
- Check if subsystem already exists before creating to avoid `BmFqnAlreadyInUseException`

### Check before creating

```java
String subsystemFqn = "Subsystem.MySubsystem";
if (transaction.getTopObjectByFqn(subsystemFqn) == null) {
    // Safe to create
} else {
    System.out.println("Subsystem already exists: " + subsystemFqn);
}
```

### Adding subsystem to parent subsystem

```java
Subsystem parentSubsystem = (Subsystem)transaction.getTopObjectByFqn("Subsystem.ParentSubsystem");
if (parentSubsystem != null) {
    Subsystem childSubsystem = mdFactory.createSubsystem();
    childSubsystem.setName("ChildSubsystem");
    childSubsystem.getSynonym().put("ru", "ДочерняяПодсистема");
    childSubsystem.setUuid(UUID.randomUUID());

    String childFqn = fqnGenerator.generateStandaloneObjectFqn(childSubsystem.eClass(), childSubsystem.getName()).toString();
    transaction.attachTopObject((IBmObject)childSubsystem, childFqn);
    configuration.getSubsystems().add(childSubsystem);
    parentSubsystem.getSubsystems().add(childSubsystem);
}
```

### Common Mistakes

**❌ WRONG - Not setting UUID**
```java
Subsystem subsystem = mdFactory.createSubsystem();
subsystem.setName("MySubsystem");
transaction.attachTopObject((IBmObject)subsystem, fqn);
// Error: SU45 - UUID required
```

**✅ CORRECT - Setting UUID manually**
```java
Subsystem subsystem = mdFactory.createSubsystem();
subsystem.setName("MySubsystem");
subsystem.setUuid(UUID.randomUUID()); // REQUIRED
transaction.attachTopObject((IBmObject)subsystem, fqn);
```

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo`. Fix only markers relevant to the changed entity before reporting success.

**Derive the `.mdo` path directly from the FQN — do not `Glob` to find it.**
For a `Subsystem.<Name>` the path is always:

```
<projectRoot>/src/Subsystems/<Name>/<Name>.mdo
```

Nested subsystems still live at the top level of `src/Subsystems/` — their FQN includes the parent (e.g. `Subsystem.Parent.Subsystem.Child`), but their `.mdo` is at `src/Subsystems/<Child>/<Child>.mdo`. Copy `<Name>` exactly from the FQN — same case, same Cyrillic. Use the project's path separator as-is (`\\` on Windows, `/` on Linux). Extension is lowercase `.mdo`. See `check_1c_markers_after_crud` for the full FQN → folder mapping.

Use project-wide markers only when the change can affect references between metadata objects or when the path truly cannot be derived.
