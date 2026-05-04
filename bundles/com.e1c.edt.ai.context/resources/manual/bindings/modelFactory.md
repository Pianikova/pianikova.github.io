## IModelObjectFactory

Higher-level creation API for project/version context.
For JShell, prefer `mdFactory` plus manual UUID assignment because `fillDefaultReferences(...)` may timeout.

```java
IV8Project v8project = projectManager.getProject(project);
Catalog catalog = (Catalog)modelFactory.create(MdClassPackage.Literals.CATALOG, v8project);
catalog.setName("Products");

CatalogAttribute attribute = (CatalogAttribute)modelFactory.create(
    MdClassPackage.Literals.CATALOG_ATTRIBUTE, catalog, v8project.getVersion());
attribute.setName("Article");
        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem stringType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.STRING);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(stringType)
            .build();

attribute.setType(typeDesc);
catalog.getAttributes().add(attribute);

// modelFactory.fillDefaultReferences(catalog); // Avoid in JShell
```
