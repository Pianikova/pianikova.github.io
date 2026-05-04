## Safe UUID Assignment for All Metadata Objects

UUIDs are CRITICAL for all metadata objects and their child elements.
Missing UUIDs cause SU45 validation errors. This workflow provides safe UUID assignment.

### Why UUIDs are critical:

- **Top-level objects**: Catalogs, Documents, Registers, etc. MUST have UUIDs
- **Child objects**: Attributes, Tabular sections, Dimensions, Resources MUST also have UUIDs
- **Validation**: SU45 error occurs if any object lacks a UUID
- **JShell**: Manual UUID assignment is RECOMMENDED (avoids OSGi timeout from fillDefaultReferences)

### Safe UUID Assignment Pattern:

```java
// Pattern 1: Safe UUID assignment with error handling
public boolean assignUuidSafely(MdObject object, String objectName) {
    try {
        object.setUuid(UUID.randomUUID());
        return true; // Success
    } catch (Exception e) {
        System.err.println("ERROR: Failed to set UUID for " + objectName + ": " + e.getMessage());
        e.printStackTrace();
        return false; // Failure
    }
}

// Pattern 2: Batch UUID assignment for child objects
public boolean assignUuidsToChildren(java.util.List<? extends MdObject> children, String parentName) {
    boolean allSuccess = true;
    for (MdObject child : children) {
        try {
            child.setUuid(UUID.randomUUID());
            System.out.println("UUID assigned to: " + parentName + "." + child.getName());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to set UUID for " + parentName + "." + child.getName() + ": " + e.getMessage());
            allSuccess = false; // Continue with remaining objects
        }
    }
    return allSuccess;
}
```

### Complete Example: Catalog with All UUIDs Safely Assigned

```java
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");

// Assign UUID to top-level object
if (!assignUuidSafely(catalog, "Catalog.Products")) {
    System.err.println("FATAL: Cannot create catalog without UUID");
    return null;
}

// Create and assign UUIDs to attributes
CatalogAttribute code = mdFactory.createCatalogAttribute();
code.setName("Code");
if (!assignUuidSafely(code, "Catalog.Products.Code")) {
    System.err.println("FATAL: Cannot create attribute without UUID");
    return null;
}

CatalogAttribute description = mdFactory.createCatalogAttribute();
description.setName("Description");
if (!assignUuidSafely(description, "Catalog.Products.Description")) {
    System.err.println("FATAL: Cannot create attribute without UUID");
    return null;
}

// Assign UUIDs to all child objects at once
java.util.List<MdObject> attributes = new java.util.ArrayList<>();
attributes.add(code);
attributes.add(description);

if (!assignUuidsToChildren(attributes, "Catalog.Products")) {
    System.err.println("WARNING: Some attributes failed UUID assignment");
    // Decide whether to continue or abort
}

// Set types and add to catalog
// ... (TypeDescription creation and assignment)
catalog.getAttributes().add(code);
catalog.getAttributes().add(description);
```

### UUID Assignment for Different Metadata Types:

**Catalog with attributes:**
```java
catalog.setUuid(UUID.randomUUID());
catalog.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));
catalog.getTabularSections().forEach(ts -> {
    ts.setUuid(UUID.randomUUID());
    ts.getAttributes().forEach(tsa -> tsa.setUuid(UUID.randomUUID()));
});
```

**Document with attributes and tabular sections:**
```java
document.setUuid(UUID.randomUUID());
document.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));
document.getTabularSections().forEach(ts -> {
    ts.setUuid(UUID.randomUUID());
    ts.getAttributes().forEach(tsa -> tsa.setUuid(UUID.randomUUID()));
});
```

**InformationRegister with dimensions and resources:**
```java
register.setUuid(UUID.randomUUID());
register.getDimensions().forEach(dim -> dim.setUuid(UUID.randomUUID()));
register.getResources().forEach(res -> res.setUuid(UUID.randomUUID()));
register.getAttributes().forEach(attr -> attr.setUuid(UUID.randomUUID()));
```

**Enum with enum values:**
```java
enumObject.setUuid(UUID.randomUUID());
enumObject.getEnumValues().forEach(value -> value.setUuid(UUID.randomUUID()));
```

**Common Pitfalls to Avoid:**
```java
// ❌ WRONG: Forgetting child object UUIDs
catalog.setUuid(UUID.randomUUID()); // OK
catalog.getAttributes().add(attr); // ❌ attr has no UUID! SU45 error!

// ❌ WRONG: Using fillDefaultReferences in JShell (may timeout)
// modelFactory.fillDefaultReferences(catalog); // DO NOT USE in JShell!

// ❌ WRONG: Not checking UUID assignment success
catalog.setUuid(UUID.randomUUID());
// If this fails, object will have null UUID and cause SU45 error

// ✅ CORRECT: Manual UUID assignment with error handling
catalog.setUuid(UUID.randomUUID());
attr.setUuid(UUID.randomUUID());
// Both objects have valid UUIDs
```

