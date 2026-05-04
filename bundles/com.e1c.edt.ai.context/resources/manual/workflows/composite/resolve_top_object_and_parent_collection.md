## Scenario: Resolve Top Object And Parent Collection

### Top-level lookup pattern
```java
Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
Document document = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
```

### Parent collection rules
- top-level objects go to `Configuration` collections like `getCatalogs()` or `getDocuments()`
- child objects go to the owning object collection like `catalog.getAttributes()`
- do not attach child objects as top-level objects

### FQN rules
- new top-level objects: generate FQN with `fqnGenerator`
- existing objects: load by known FQN and mutate in-place
- do not call `attachTopObject()` on objects already present in the model
