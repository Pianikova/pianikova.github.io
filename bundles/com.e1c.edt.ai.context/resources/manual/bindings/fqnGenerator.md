## ITopObjectFqnGenerator

Generates FQN for top-level metadata objects before `attachTopObject`. Required when creating NEW metadata objects.

```java
// CORRECT: Generate FQN for new object
Catalog catalog = mdFactory.createCatalog();
catalog.setName("Products");
String fqn = fqnGenerator
    .generateStandaloneObjectFqn(catalog.eClass(), catalog.getName())
    .toString();
transaction.attachTopObject((IBmObject)catalog, fqn);
```

**⚠️ NOT needed for editing existing objects:**
```java
// Get existing object - FQN already known
Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products");
// No need to generate FQN!
```

**FQN Format Examples:**
- `Catalog.Products`
- `Document.GoodsReceipt`
- `InformationRegister.ExchangeRates`
- `AccumulationRegister.GoodsInStock`
