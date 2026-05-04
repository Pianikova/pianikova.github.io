## Safe Workflow: Delete 1C Configuration Project

This workflow permanently deletes a 1C:Enterprise configuration project from the workspace.
⚠️ **WARNING:** This operation cannot be undone. All project data will be permanently deleted.

### Part 1: Dissociate Infobases (if any)

```java
// Step 1: Get the project to delete
String projectName = "MyConfigurationToDelete";
IProject project = workspaceRoot.getProject(projectName);

// Step 2: Check if project exists
if (project.exists()) {

    // Step 3: Dissociate infobases before deleting (if they exist)
    // Note: IInfobaseAssociationManager must be injected via dependency injection
    // This example shows the workflow pattern

/*
IV8Project v8project = projectManager.getProject(project);
IInfobaseAssociationManager associationManager = getInfobaseAssociationManager();

try {
    Optional<IInfobaseAssociation> association = associationManager.getAssociation(project);

    if (association.isPresent()) {
        Collection<InfobaseReference> infobases = association.get().getInfobases();
        InfobaseAssociationContext context = association.get().getContext();

        for (InfobaseReference infobase : infobases) {
            // Dissociate each infobase
            associationManager.dissociate(project, infobase, context);
            System.out.println("Dissociated infobase: " + infobase.getName());
        }
    }
} catch (InfobaseAssociationException e) {
    System.err.println("Error dissociating infobases: " + e.getMessage());
}
*/

```
### Part 2: Delete the Project
```java
    // Step 4: Close the project if it's open
    if (project.isOpen()) {
        project.close(new NullProgressMonitor());
    }

    // Step 5: Delete the project (true = force delete, true = delete content on disk)
    project.delete(true, true, new NullProgressMonitor());

    System.out.println("Configuration project deleted successfully: " + projectName);
} else {
    System.out.println("Project does not exist: " + projectName);
}
```
### Part 3: Optional - Delete Infobase from Registry
```java
// Step 6: Optionally delete infobase reference from the registry
// Note: This removes the infobase from EDT's infobase list
// This requires IInfobaseManager and should be done carefully

/*
// IInfobaseManager infobaseManager = getInfobaseManager();
// List<Section> allSections = infobaseManager.getAll();
// List<InfobaseReference> allInfobases = InfobaseReferences.asPlainList(allSections);
//
// for (InfobaseReference infobase : allInfobases) {
//     if (infobase.getName().equals("MyInfobaseName")) {
//         infobaseManager.delete(infobase);
//         System.out.println("Infobase deleted from registry: " + infobase.getName());
//         break;
//     }
// }
*/
```
**Important Notes:**
**Part 1 - Infobase Dissociation:**
- Dissociating infobases before project deletion is RECOMMENDED to clean up associations
- This requires IInfobaseAssociationManager (injected via dependency injection)
- Dissociation fires events that notify listeners (e.g., application deletion notifications)
- If infobases are not dissociated, they remain in the association store (garbage data)
**Part 2 - Project Deletion:**
- First parameter (true): Force delete - deletes project even if resources are locked
- Second parameter (true): Delete content on disk - removes files from filesystem
- Set second parameter to false if you want to keep the project files
- This operation cannot be undone - ensure you have backups if needed
- For production use, use proper IProgressMonitor implementation
- Close the project before deleting to avoid resource locks
- Check for open editors and unsaved changes before deletion
- Verify no other projects reference this project
**Part 3 - Infobase Registry Cleanup (Optional):**
- This removes infobase from EDT's infobase registry list
- Use only if you want to completely remove the infobase
- Requires IInfobaseManager (injected via dependency injection)
- Be careful: this affects all projects that use this infobase
**Alternative: Delete without removing disk files**
```java
// This removes the project from workspace but keeps files on disk
project.delete(true, false, new NullProgressMonitor());
```
**Complete Safe Workflow (recommended for production):**
```java
public void deleteConfigurationProject(String projectName) {
    IProject project = workspaceRoot.getProject(projectName);

    if (!project.exists()) {
        System.out.println("Project does not exist: " + projectName);
        // Stop here in JShell because the project does not exist.
    }

    try {
        // Step 1: Get IV8Project
        IV8Project v8project = projectManager.getProject(project);

        // Step 2: Dissociate infobases (if any)
        // Optional: depends on whether infobase management is needed
/*
        Optional<IInfobaseAssociation> association = associationManager.getAssociation(project);
        if (association.isPresent()) {
            for (InfobaseReference infobase : association.get().getInfobases()) {
                associationManager.dissociate(project, infobase, association.get().getContext());
            }
        }
*/

        // Step 3: Check for open editors and unsaved changes
        // This is important to prevent data loss during deletion
        // Note: In production code, you should check for open editors in the workbench
        // and prompt the user to save unsaved changes

        // Step 4: Close project
        if (project.isOpen()) {
            project.close(new NullProgressMonitor());
        }

        // Step 5: Delete project
        project.delete(true, true, new NullProgressMonitor());

        System.out.println("Configuration project deleted successfully: " + projectName);

    } catch (CoreException e) {
        System.err.println("Error deleting project: " + e.getMessage());
        e.printStackTrace();
    }
});
```
**Note:** After creating the common module metadata, create the corresponding Module.bsl file
in the project: `src/CommonModules/<ModuleName>/Module.bsl`
**Deletion:** Common modules can be deleted by removing from configuration.getCommonModules()
and detaching from transaction. Remove the module from configuration.getCommonModules() and detach it from the transaction.
**Properties explanation:**
- `server`: Execution on server side
- `clientManagedApplication`: Execution in managed application client
- `clientOrdinaryApplication`: Execution in ordinary application client
- `serverCall`: Allow calls from client to server
- `externalConnection`: Execution in external connection
- `privileged`: Execution in privileged mode
- `global`: Export module functions to global context
