## Safe Workflow: Create Enum

`Enum` conflicts with `java.lang.Enum` in JShell. Always use the fully-qualified
EDT type `com._1c.g5.v8.dt.metadata.mdclass.Enum` in variable declarations,
generic parameters, casts, and return types. `EnumValue` is safe to qualify too.

```java
IProject project = workspaceRoot.getProject("MyProject");
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<com._1c.g5.v8.dt.metadata.mdclass.Enum>("Create enum") {
    @Override
    public com._1c.g5.v8.dt.metadata.mdclass.Enum execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        com._1c.g5.v8.dt.metadata.mdclass.Enum enumObject = mdFactory.createEnum();
        enumObject.setName("Statuses");
        enumObject.setUuid(UUID.randomUUID());
        com._1c.g5.v8.dt.metadata.mdclass.EnumValue active = mdFactory.createEnumValue();
        active.setName("Active");
        active.setUuid(UUID.randomUUID());
        enumObject.getEnumValues().add(active);
        String fqn = fqnGenerator.generateStandaloneObjectFqn(enumObject.eClass(), enumObject.getName()).toString();
        transaction.attachTopObject((IBmObject)enumObject, fqn);
        configuration.getEnums().add(enumObject);
        return enumObject;
    }
});
```

### Notes
- Add at least one `EnumValue` immediately to reduce validation issues
- Set UUID on the enum and on each enum value
- For edits, mutate `enumObject.getEnumValues()` directly instead of recreating the enum
- Never write `Enum enumObject` or `new AbstractBmTask<Enum>(...)` in JShell; it is ambiguous with `java.lang.Enum`

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` and `path` to the changed top-level `.mdo` when known or derivable. Fix only markers relevant to the changed entity before reporting success. Use project-wide markers only for affected references or when the path cannot be derived.
