## Safe Workflow: Create ${title}

```java
IProject project = workspaceRoot.getProject("MyProject");
${extraSetup}IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();
globalContext.execute(new AbstractBmTask<Void>("Create ${title}") {
    @Override
    public Void execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
        ${typeName} ${variableName} = mdFactory.${createMethod};
        ${variableName}.setName("${sampleName}");
        ${variableName}.setUuid(UUID.randomUUID());
${setupBlock}        String fqn = fqnGenerator.generateStandaloneObjectFqn(${variableName}.eClass(), ${variableName}.getName()).toString();
        transaction.attachTopObject((IBmObject)${variableName}, fqn);
        configuration.${collection}.add(${variableName});
        return null;
    }
});
```

### Rules
- `manual_ids` do not execute imports from manual cards. If `${typeName}` or
  another scenario-specific class is not already imported in the JShell
  session, add an explicit import in the same snippet or use the fully
  qualified class name. See `jshell_edt_canonical_imports`.
- If this exact scenario/manual card gives the factory, collection, FQN prefix, and safe setters you need, do not call `JShellReflection` before using it
- Before calling `JShellReflection`, check the EDT metadata API cards for the requested type
- If the API card lists the factory, collection, and setters you need, use this manual workflow without reflection
- Create the object only inside BM transaction
- Set UUID before attach
- Generate FQN with `fqnGenerator`
- Add the object to `Configuration.${collection}`
- For the delete phase of CRUD, switch to `delete_metadata_object`. Do not
  delete top-level objects with `Configuration.${collection}.remove(...)` and
  `transaction.detachTopObject(...)`.
- For read/verify snippets that return `String` or another value from
  `globalContext.execute(...)`, assign it to a variable and print it with
  `System.out.println(...)`; returning a value alone does not appear in
  `std_out`.
- If `std_out` or the tool response names a different metadata object/type
  than the current create request, treat the JShell session as stale. Create a
  fresh `jshellsession` and rerun the operation with explicit imports before
  continuing.
- If this workflow requires more than one unknown EDT type, method, factory, field, or enum, call `JShellReflection` once with the full `queries` array before writing JShell code
- Call `JShell` with `scope: "edt"`, `request_description`, and `response_description`
- Mandatory next tool after this JShell create: run `GetMarkers` with `marker_type: "1c"` and `path` to the created top-level `.mdo` when the path is known or can be derived
- Derive marker paths with the top-level layout
  `<projectRoot>/src/<TypePluralFolder>/<Name>/<Name>.mdo`. Never omit the
  intermediate `<Name>` folder. For example, `DocumentJournal.ЖурналПродаж`
  is `src/DocumentJournals/ЖурналПродаж/ЖурналПродаж.mdo`, not
  `src/DocumentJournals/ЖурналПродаж.mdo`. See
  `check_1c_markers_after_crud` for the full folder map.
- Do not report success and do not start the next 1C metadata CRUD operation until the `GetMarkers` response is checked
- Inspect all relevant 1C markers for the changed entity/top object, including errors, warnings, and infos; do not check only errors
- Do not fix unrelated project-wide markers. Use project-wide `GetMarkers` only for references, registrars, command interfaces, configuration-level changes, or when the `.mdo` path cannot be derived.
- If `GetMarkers` returns relevant validation markers, treat the operation as incomplete until they are fixed or explicitly explained

### Notes
- ${notes}
